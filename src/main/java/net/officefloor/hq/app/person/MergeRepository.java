package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.HistoryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The last merge into each person, kept so it can be undone (see V116__person_merge_undo.sql). */
@Repository
public class MergeRepository {

    /** The last merge into a person: who made it, on which sheet-today, and who was merged. */
    public record Merge(long id, long fromId, String by, LocalDate on) {
    }

    private final JdbcTemplate jdbc;
    private final HistoryRepository history;

    public MergeRepository(JdbcTemplate jdbc, HistoryRepository history) {
        this.jdbc = jdbc;
        this.history = history;
    }

    /** Keep a merge about to be made, as the last into {@code intoId} (replacing any before it): the merged person as
     *  they are now (row, dated grades and terms, aliases) and their claims. Call before the claims and person move. */
    public void keep(String sheetId, long fromId, long intoId, String by, LocalDate on) {
        forget(sheetId, intoId);
        // A merge into the merged person can no longer be undone: they leave the sheet.
        forget(sheetId, fromId);
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement("INSERT INTO person_merge (sheet_id, into_id, from_id, merged_by,"
                    + " merged_on) VALUES (?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, intoId);
            ps.setLong(3, fromId);
            ps.setString(4, by);
            ps.setDate(5, Date.valueOf(on));
            return ps;
        }, keys);
        long mergeId = keys.getKey().longValue();
        String columns = String.join(", ", personColumns());
        jdbc.update("INSERT INTO person_merge_row (" + columns + ", merge_id) SELECT " + columns + ", ? FROM person"
                + " WHERE id = ? AND sheet_id = ?", mergeId, fromId, sheetId);
        jdbc.update("INSERT INTO person_merge_grade (merge_id, grade, from_date) SELECT ?, grade, from_date FROM person_grade"
                + " WHERE sheet_id = ? AND person_id = ?", mergeId, sheetId, fromId);
        jdbc.update("INSERT INTO person_merge_terms (merge_id, terms, from_date) SELECT ?, terms, from_date FROM person_terms"
                + " WHERE sheet_id = ? AND person_id = ?", mergeId, sheetId, fromId);
        jdbc.update("INSERT INTO person_merge_alias (merge_id, seq, alias) SELECT ?, 0, name FROM person"
                + " WHERE id = ? AND sheet_id = ?", mergeId, fromId, sheetId);
        List<String> aliases = jdbc.queryForList("SELECT alias FROM person_alias WHERE sheet_id = ? AND person_id = ?"
                + " ORDER BY id", String.class, sheetId, fromId);
        for (int i = 0; i < aliases.size(); i++) {
            jdbc.update("INSERT INTO person_merge_alias (merge_id, seq, alias) VALUES (?, ?, ?)", mergeId, i + 1, aliases.get(i));
        }
        jdbc.update("INSERT INTO person_merge_claim (merge_id, claim_id) SELECT ?, id FROM claim"
                + " WHERE sheet_id = ? AND person_id = ?", mergeId, sheetId, fromId);
    }

    /** The last merge into a person still to undo; null when there is none. */
    public Merge last(String sheetId, long intoId) {
        List<Merge> found = jdbc.query("SELECT id, from_id, merged_by, merged_on FROM person_merge WHERE sheet_id = ?"
                + " AND into_id = ? ORDER BY id DESC LIMIT 1", (rs, i) -> new Merge(rs.getLong("id"), rs.getLong("from_id"),
                rs.getString("merged_by"), rs.getDate("merged_on").toLocalDate()), sheetId, intoId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The merged person's name, as it was when merged. */
    public String name(Merge merge) {
        return jdbc.queryForObject("SELECT alias FROM person_merge_alias WHERE merge_id = ? AND seq = 0", String.class,
                merge.id());
    }

    /** Whether a name the merge would give back (the merged person's name or aliases) is now another person's than
     *  {@code intoId}'s — or theirs other than as one the merge brought them. */
    public boolean namesTaken(String sheetId, long intoId, Merge merge) {
        Integer taken = jdbc.queryForObject("SELECT COUNT(*) FROM person_merge_alias m WHERE m.merge_id = ? AND ("
                + " EXISTS (SELECT 1 FROM person p WHERE p.sheet_id = ? AND LOWER(p.name) = LOWER(m.alias))"
                + " OR EXISTS (SELECT 1 FROM person_alias a WHERE a.sheet_id = ? AND a.person_id <> ?"
                + " AND LOWER(a.alias) = LOWER(m.alias)))", Integer.class, merge.id(), sheetId, sheetId, intoId);
        return taken != null && taken > 0;
    }

    /** Undo a merge into {@code intoId}: the merged person is back (same id) at the end of the list with their grades,
     *  terms and own aliases, which leave the other's; the claims that came from them and are still the other's go
     *  back to them as they are now (listed in the order they were added). Patrons, pools, crews, periods' figures,
     *  statements and plans stay as the merge left them. Nothing more is left to undo for {@code intoId}. */
    public void undo(String sheetId, long intoId, Merge merge) {
        String columns = String.join(", ", personColumns());
        jdbc.update("INSERT INTO person (" + columns + ") SELECT " + columns + " FROM person_merge_row WHERE merge_id = ?",
                merge.id());
        jdbc.update("UPDATE person SET list_position = (SELECT COALESCE(MAX(q.list_position), 0) + 1 FROM person q"
                + " WHERE q.sheet_id = ?) WHERE id = ? AND sheet_id = ?", sheetId, merge.fromId(), sheetId);
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) SELECT ?, ?, grade, from_date"
                + " FROM person_merge_grade WHERE merge_id = ?", sheetId, merge.fromId(), merge.id());
        jdbc.update("INSERT INTO person_terms (sheet_id, person_id, terms, from_date) SELECT ?, ?, terms, from_date"
                + " FROM person_merge_terms WHERE merge_id = ?", sheetId, merge.fromId(), merge.id());

        // The names the merge brought leave the other's aliases (the latest of each); their own go back to them.
        List<Map<String, Object>> names = jdbc.queryForList("SELECT seq, alias FROM person_merge_alias WHERE merge_id = ?"
                + " ORDER BY seq", merge.id());
        for (Map<String, Object> n : names) {
            String alias = (String) n.get("alias");
            List<Long> ids = jdbc.queryForList("SELECT id FROM person_alias WHERE sheet_id = ? AND person_id = ? AND alias = ?"
                    + " ORDER BY id DESC LIMIT 1", Long.class, sheetId, intoId, alias);
            if (!ids.isEmpty()) jdbc.update("DELETE FROM person_alias WHERE id = ?", ids.get(0));
            if (((Number) n.get("seq")).intValue() > 0) {
                jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) VALUES (?, ?, ?)", sheetId, merge.fromId(), alias);
            }
        }

        List<Long> claims = new ArrayList<>(jdbc.queryForList("SELECT c.id FROM claim c JOIN person_merge_claim m"
                + " ON m.claim_id = c.id WHERE m.merge_id = ? AND c.sheet_id = ? AND c.person_id = ? ORDER BY c.listed, c.id",
                Long.class, merge.id(), sheetId, intoId));
        for (Long claimId : claims) {
            if (jdbc.update("UPDATE claim SET person_id = ?, listed = NEXT VALUE FOR claim_listed_seq WHERE id = ?"
                    + " AND sheet_id = ?", merge.fromId(), claimId, sheetId) > 0) {
                history.claim(sheetId, claimId);
            }
        }
        forget(sheetId, intoId);
    }

    /** Drop whatever merge into a person was kept to undo. */
    public void forget(String sheetId, long intoId) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM person_merge WHERE sheet_id = ? AND into_id = ?", Long.class,
                sheetId, intoId);
        for (Long id : ids) {
            jdbc.update("DELETE FROM person_merge_row WHERE merge_id = ?", id);
            jdbc.update("DELETE FROM person_merge_grade WHERE merge_id = ?", id);
            jdbc.update("DELETE FROM person_merge_terms WHERE merge_id = ?", id);
            jdbc.update("DELETE FROM person_merge_alias WHERE merge_id = ?", id);
            jdbc.update("DELETE FROM person_merge_claim WHERE merge_id = ?", id);
            jdbc.update("DELETE FROM person_merge WHERE id = ?", id);
        }
    }

    /** The person columns a merge keeps and gives back: every one the person table stores (not computed) that the
     *  kept row has too. */
    private List<String> personColumns() {
        return jdbc.queryForList("SELECT p.COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS p WHERE p.TABLE_SCHEMA = 'PUBLIC'"
                + " AND p.TABLE_NAME = 'PERSON' AND p.IS_GENERATED = 'NEVER' AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS r"
                + " WHERE r.TABLE_SCHEMA = 'PUBLIC' AND r.TABLE_NAME = 'PERSON_MERGE_ROW' AND r.COLUMN_NAME = p.COLUMN_NAME)"
                + " ORDER BY p.ORDINAL_POSITION", String.class);
    }
}
