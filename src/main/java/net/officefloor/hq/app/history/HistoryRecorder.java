package net.officefloor.hq.app.history;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.AfterCompletionCallback;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.BeforeCompletionCallback;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Records into entity_history the state each kept entity (see History) is in once a change to it is saved: as the
 * transaction completes, one state per entity it inserted, updated (its fields or its collections) or deleted (no
 * state), each with its sheet's today then.
 */
@Component
public class HistoryRecorder implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener,
        PostCollectionUpdateEventListener, PostCollectionRecreateEventListener {

    /** The entities each open session has changed, by kind and id, in the order first changed: each mapped to
     *  whether it was deleted. */
    private final Map<SharedSessionContractImplementor, Map<Object, Boolean>> changed =
            Collections.synchronizedMap(new IdentityHashMap<>());

    /** The date a change is to be recorded on instead of its sheet's today, by entity. */
    private final Map<Object, java.time.LocalDate> recordedOn = Collections.synchronizedMap(new IdentityHashMap<>());

    private final EntityManagerFactory entityManagerFactory;

    private final History history;

    public HistoryRecorder(EntityManagerFactory entityManagerFactory, History history) {
        this.entityManagerFactory = entityManagerFactory;
        this.history = history;
    }

    @PostConstruct
    void register() {
        EventListenerRegistry registry = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getEventListenerRegistry();
        registry.appendListeners(EventType.POST_INSERT, this);
        registry.appendListeners(EventType.POST_UPDATE, this);
        registry.appendListeners(EventType.POST_DELETE, this);
        registry.appendListeners(EventType.POST_COLLECTION_UPDATE, this);
        registry.appendListeners(EventType.POST_COLLECTION_RECREATE, this);
    }

    /** Records the entity's next saved state on the date, not its sheet's today then. */
    public void recordOn(Object entity, java.time.LocalDate date) {
        recordedOn.put(entity, date);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        note(event.getSession(), event.getEntity(), false);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        note(event.getSession(), event.getEntity(), false);
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        note(event.getSession(), event.getEntity(), true);
    }

    @Override
    public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
        noteOwner(event);
    }

    @Override
    public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
        noteOwner(event);
    }

    private void noteOwner(AbstractCollectionEvent event) {
        Object owner = event.getAffectedOwnerOrNull();
        if (owner != null) {
            note(event.getSession(), owner, false);
        }
    }

    private void note(SharedSessionContractImplementor session, Object entity, boolean deleted) {
        if (History.kindOf(entity.getClass()) == null) {
            return;
        }
        Map<Object, Boolean> mine = changed.get(session);
        if (mine == null) {
            Map<Object, Boolean> fresh = new LinkedHashMap<>();
            changed.put(session, fresh);
            mine = fresh;
            session.getTransactionCompletionCallbacks().registerCallback((BeforeCompletionCallback) s -> {
                Map<Object, Boolean> done = changed.remove(s);
                if (done != null && !done.isEmpty()) {
                    s.doWork(connection -> write(connection, done));
                }
            });
            session.getTransactionCompletionCallbacks()
                    .registerCallback((AfterCompletionCallback) (success, s) -> changed.remove(s));
        }
        // An entity is keyed by itself: one instance per id in a session.
        mine.merge(entity, deleted, (was, now) -> was || now);
    }

    private void write(Connection connection, Map<Object, Boolean> done) throws SQLException {
        Map<String, java.sql.Date> todays = new HashMap<>();
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO entity_history (sheet_id, kind, entity_id, made_on, state) VALUES (?, ?, ?, ?, ?)")) {
            for (Map.Entry<Object, Boolean> e : done.entrySet()) {
                Object entity = e.getKey();
                JsonNode state = history.stateOf(entity);
                if (state.get("id") == null || state.get("id").isNull()) {
                    continue;
                }
                long id = state.get("id").asLong();
                String sheetId = sheetOf(connection, state);
                if (sheetId == null) {
                    continue;
                }
                java.sql.Date today = todays.computeIfAbsent(sheetId, k -> todayOf(connection, k));
                java.time.LocalDate on = recordedOn.remove(entity);
                if (today == null) {
                    continue;
                }
                if (on != null) {
                    today = java.sql.Date.valueOf(on);
                }
                String kind = History.kindOf(entity.getClass());
                if ("claim".equals(kind) && state instanceof ObjectNode claim) {
                    // A claim's listing key is given by the store, so its saved copy may not hold it.
                    claim.put("listed", listedOf(connection, id));
                }
                insert.setString(1, sheetId);
                insert.setString(2, kind);
                insert.setLong(3, id);
                insert.setDate(4, today);
                if (e.getValue()) {
                    insert.setNull(5, Types.CLOB);
                } else {
                    insert.setString(5, history.write(state));
                }
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    /** The sheet the state is of: its own, or (a pool's or a crew's member, which holds none) its pool's or crew's. */
    private static String sheetOf(Connection connection, JsonNode state) throws SQLException {
        if (state.get("sheetId") != null && !state.get("sheetId").isNull()) {
            return state.get("sheetId").asString();
        }
        String sql;
        JsonNode owner;
        if (state.get("poolId") != null) {
            sql = "SELECT sheet_id FROM pool WHERE id = ?";
            owner = state.get("poolId");
        } else if (state.get("crewId") != null) {
            sql = "SELECT sheet_id FROM crew WHERE id = ?";
            owner = state.get("crewId");
        } else {
            return null;
        }
        if (owner.isNull()) {
            return null;
        }
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            select.setLong(1, owner.asLong());
            try (ResultSet rs = select.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static java.sql.Date todayOf(Connection connection, String sheetId) {
        try (PreparedStatement select = connection.prepareStatement("SELECT today FROM sheet WHERE id = ?")) {
            select.setString(1, sheetId);
            try (ResultSet rs = select.executeQuery()) {
                return rs.next() ? rs.getDate(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static long listedOf(Connection connection, long claimId) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT listed FROM claim WHERE id = ?")) {
            select.setLong(1, claimId);
            try (ResultSet rs = select.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }
}
