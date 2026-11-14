package net.officefloor.hq.app.person;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * The merges made through the row, each keeping the merged person's own rows as they stood and the claims that came
 * from them, so the merge can be undone.
 */
@Repository
public class MergeRepository {

    /** The merged person's own tables kept under a merge (each also kept as {@code merged_<table>}), person first. */
    private static final List<String> KEPT = List.of("person", "person_grade", "person_terms", "person_hold",
            "person_retirement", "person_watch", "person_alias");

    /** The position at the end of the sheet's list (the sheet id its one parameter). */
    private static final String NEXT_POSITION = "(SELECT COALESCE(MAX(position), 0) + 1 FROM person WHERE sheet_id = ?)";

    private final JdbcTemplate jdbc;

    public MergeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Record the merge about to be made by the user on the sheet's today: the merged person's own rows as they stand
     * and their claims with the place each is listed at. Call before the merge moves or drops them.
     */
    public void record(Merge merge, String user, LocalDate today) {
        String sheetId = merge.from().sheetId();
        long from = merge.from().id();
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("INSERT INTO person_merge (sheet_id, person_id, into_id, by_user,"
                    + " made_on) VALUES (?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, from);
            ps.setLong(3, merge.into().id());
            ps.setString(4, user);
            ps.setDate(5, Date.valueOf(today));
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        for (String table : KEPT) {
            List<String> columns = columns(table, "merged_" + table);
            String list = String.join(", ", columns);
            jdbc.update("INSERT INTO merged_" + table + " (" + list + ", merge_id) SELECT " + list + ", ? FROM " + table
                    + " WHERE sheet_id = ? AND " + ("person".equals(table) ? "id" : "person_id") + " = ?", id, sheetId,
                    from);
        }
        jdbc.update("INSERT INTO person_merge_claim (sheet_id, merge_id, claim_id, listed) SELECT sheet_id, ?, id, listed"
                + " FROM claim WHERE sheet_id = ? AND person_id = ?", id, sheetId, from);
    }

    /** The last merge into the person not yet undone, or null when there is none. */
    public MergeMade last(String sheetId, long intoId) {
        List<MergeMade> found = jdbc.query("SELECT m.id, m.person_id, m.by_user, m.made_on, p.name FROM person_merge m"
                + " JOIN merged_person p ON p.merge_id = m.id WHERE m.sheet_id = ? AND m.into_id = ? AND NOT m.undone"
                + " ORDER BY m.id DESC LIMIT 1", (rs, n) -> new MergeMade(rs.getLong("id"), rs.getLong("person_id"),
                        rs.getString("name"), rs.getString("by_user"), rs.getObject("made_on", LocalDate.class)),
                sheetId, intoId);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Undo the merge into the person: the merged person is back, at the end of the list, as they stood (with no
     * statements, those staying with the other), with the claims that came from them still with the other (as they
     * are now, listed where they were) and their own aliases, which (with their name) leave the other's.
     */
    public void undo(MergeMade made, Person into) {
        String sheetId = into.sheetId();
        long id = made.id();
        for (String table : KEPT) {
            List<String> columns = columns("merged_" + table, table);
            String list = String.join(", ", columns);
            jdbc.update("INSERT INTO " + table + " (" + list + ") SELECT " + list + " FROM merged_" + table
                    + " WHERE merge_id = ?", id);
            if ("person".equals(table)) {
                jdbc.update("UPDATE person SET position = " + NEXT_POSITION + ", statement_count = 0,"
                        + " stated_total = NULL, statement_date = NULL WHERE sheet_id = ? AND id = ?", sheetId, sheetId,
                        made.personId());
            }
            jdbc.update("DELETE FROM merged_" + table + " WHERE merge_id = ?", id);
        }
        List<String> given = new java.util.ArrayList<>();
        given.add(made.name());
        given.addAll(jdbc.queryForList("SELECT alias FROM person_alias WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                String.class, sheetId, made.personId()));
        for (String alias : given) {
            jdbc.update("DELETE FROM person_alias WHERE id = (SELECT MAX(id) FROM person_alias WHERE sheet_id = ? AND"
                    + " person_id = ? AND alias = ?)", sheetId, into.id(), alias);
        }
        jdbc.update("UPDATE claim c SET person_id = ?, listed = (SELECT m.listed FROM person_merge_claim m WHERE"
                + " m.merge_id = ? AND m.claim_id = c.id) WHERE c.sheet_id = ? AND c.person_id = ? AND c.id IN"
                + " (SELECT claim_id FROM person_merge_claim WHERE merge_id = ?)", made.personId(), id, sheetId,
                into.id(), id);
        jdbc.update("DELETE FROM person_merge_claim WHERE merge_id = ?", id);
        jdbc.update("UPDATE person_merge SET undone = TRUE WHERE id = ?", id);
    }

    /** The columns a copy from one table to the other writes: the source's, but its generated ones, the target has. */
    private List<String> columns(String source, String target) {
        return jdbc.queryForList("SELECT s.column_name FROM information_schema.columns s WHERE s.table_schema = 'PUBLIC'"
                + " AND s.table_name = ? AND s.is_generated = 'NEVER' AND s.column_name <> 'MERGE_ID' AND EXISTS"
                + " (SELECT 1 FROM information_schema.columns t WHERE t.table_schema = 'PUBLIC' AND t.table_name = ?"
                + " AND t.column_name = s.column_name AND t.is_generated = 'NEVER') ORDER BY s.ordinal_position",
                String.class, source.toUpperCase(Locale.ROOT), target.toUpperCase(Locale.ROOT));
    }
}
