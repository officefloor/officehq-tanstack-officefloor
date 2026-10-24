package net.officefloor.hq.app.history;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.person.Patron;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGrade;
import net.officefloor.hq.app.role.Delegation;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.statement.Statement;
import net.officefloor.hq.app.watch.Watch;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The states a sheet's things have been saved in (entity_history), each with the sheet's today when it was saved —
 * recorded by HistoryRecorder — and read back as they stood at the end of a date: the last state saved while the
 * sheet's today was on or before it. A thing with no state saved by then did not exist then (or was deleted); one
 * with no state recorded at all was saved before this was kept and stands as it is.
 */
@Service
public class History {

    /** The kind each kept entity is recorded under. */
    private static final Map<Class<?>, String> KINDS = Map.of(Claim.class, "claim", Person.class, "person",
            PersonGrade.class, "person-grade", Period.class, "period", Patron.class, "patron", Watch.class, "watch",
            Role.class, "role", Delegation.class, "delegation", Statement.class, "statement");

    /** An entity's state as its fields hold it: every field, no getter. */
    private final JsonMapper json = JsonMapper.builder()
            .changeDefaultVisibility(v -> v.withVisibility(PropertyAccessor.ALL, Visibility.NONE)
                    .withVisibility(PropertyAccessor.FIELD, Visibility.ANY))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final JdbcTemplate jdbc;

    public History(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The kind an entity is recorded under; null for one that is not kept. */
    static String kindOf(Class<?> type) {
        return KINDS.get(type);
    }

    /** The entity's state, as recorded. */
    JsonNode stateOf(Object entity) {
        return json.valueToTree(entity);
    }

    String write(JsonNode state) {
        return json.writeValueAsString(state);
    }

    /**
     * The sheet's things of a type as they stood at the end of a date, in no particular order: each as its last
     * state saved while the sheet's today was on or before it; none deleted or not yet made by then. A thing with no
     * state recorded at all is taken as it stands now. Only worked out, never saved.
     *
     * @param current the sheet's things of the type as they stand now.
     */
    public <T> List<T> asOf(String sheetId, Class<T> type, LocalDate date, List<T> current, Function<T, Long> idOf) {
        Set<Long> recorded = new HashSet<>();
        Map<Long, String> then = new LinkedHashMap<>();
        jdbc.query("SELECT entity_id, made_on, state FROM entity_history WHERE sheet_id = ? AND kind = ? ORDER BY id",
                rs -> {
                    long id = rs.getLong(1);
                    recorded.add(id);
                    Date madeOn = rs.getDate(2);
                    if (!madeOn.toLocalDate().isAfter(date)) {
                        String state = rs.getString(3);
                        if (state == null) {
                            then.remove(id);
                        } else {
                            then.put(id, state);
                        }
                    }
                }, sheetId, kindOf(type));
        List<T> asOf = new ArrayList<>();
        for (T now : current) {
            if (!recorded.contains(idOf.apply(now))) {
                asOf.add(now);
            }
        }
        then.values().forEach(state -> asOf.add(json.readValue(state, type)));
        return asOf;
    }
}
