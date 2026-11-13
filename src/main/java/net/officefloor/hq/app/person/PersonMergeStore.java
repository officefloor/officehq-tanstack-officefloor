package net.officefloor.hq.app.person;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The merges made on each sheet, kept so the last merge into a person can be undone: what the merged person was
 *  (their row, grades and dated terms), the aliases that moved with them and the claims that came from them. */
@Repository
public class PersonMergeStore {

    /** The last merge into a person: who was merged, by whom, on which sheet-today, and whether it is undone. */
    public record Merge(long id, long fromId, long intoId, String by, LocalDate on, boolean undone, String name) {
    }

    private final JdbcTemplate jdbc;

    public PersonMergeStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keeps the person as they are before they are merged into {@code into}: call before anything moves. The id
     *  of the merge. */
    public long begin(Person person, Person into, String by, LocalDate on) {
        String sheetId = person.getSheetId();
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("INSERT INTO person_merge (sheet_id, from_id, into_id, merged_by,"
                    + " merged_on, name, terms, contact, tags, origin, arrived_on, version)"
                    + " SELECT sheet_id, id, ?, ?, ?, name, terms, contact, tags, origin, arrived_on, version FROM person"
                    + " WHERE sheet_id = ? AND id = ?", new String[] { "ID" });
            ps.setLong(1, into.getId());
            ps.setString(2, by);
            ps.setObject(3, on);
            ps.setString(4, sheetId);
            ps.setLong(5, person.getId());
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        jdbc.update("INSERT INTO person_merge_grade (merge_id, from_date, grade) SELECT ?, from_date, grade"
                + " FROM person_grade WHERE sheet_id = ? AND person_id = ?", id, sheetId, person.getId());
        jdbc.update("INSERT INTO person_merge_terms (merge_id, from_date, terms) SELECT ?, from_date, terms"
                + " FROM person_terms WHERE sheet_id = ? AND person_id = ?", id, sheetId, person.getId());
        jdbc.update("INSERT INTO person_merge_alias (merge_id, alias_id) SELECT ?, id"
                + " FROM person_alias WHERE sheet_id = ? AND person_id = ?", id, sheetId, person.getId());
        jdbc.update("INSERT INTO person_merge_claim (merge_id, claim_id) SELECT ?, id"
                + " FROM claim WHERE sheet_id = ? AND person_id = ?", id, sheetId, person.getId());
        return id;
    }

    /** Notes the alias the merged person's name became, once the merge has made it. */
    public void finish(long mergeId, Person person, Person into) {
        jdbc.update("UPDATE person_merge SET name_alias_id = (SELECT MAX(id) FROM person_alias WHERE sheet_id = ?"
                + " AND person_id = ? AND alias = ?) WHERE id = ?", person.getSheetId(), into.getId(), person.getName(),
                mergeId);
    }

    /** The last merge into the person; null when none was made. */
    public Merge last(Person into) {
        List<Merge> found = jdbc.query("SELECT * FROM person_merge WHERE sheet_id = ? AND into_id = ?"
                + " ORDER BY id DESC LIMIT 1", (rs, i) -> new Merge(rs.getLong("id"), rs.getLong("from_id"),
                        rs.getLong("into_id"), rs.getString("merged_by"), rs.getObject("merged_on", LocalDate.class),
                        rs.getBoolean("undone"), rs.getString("name")), into.getSheetId(), into.getId());
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether a person on the sheet other than {@code into} has the name, or an alias not moving back has it. */
    public boolean nameTaken(String sheetId, Merge merge) {
        Integer n = jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?))"
                + " + (SELECT COUNT(*) FROM person_alias WHERE sheet_id = ? AND alias_key = LOWER(?) AND id NOT IN"
                + " (SELECT alias_id FROM person_merge_alias WHERE merge_id = ?) AND id <> COALESCE((SELECT name_alias_id"
                + " FROM person_merge WHERE id = ?), -1))", Integer.class, sheetId, merge.name(), sheetId, merge.name(),
                merge.id(), merge.id());
        return n != null && n > 0;
    }

    /** Undoes the merge: the merged person is back on the sheet under their id, at the end of the list, with their
     *  grades and dated terms, the claims that came from them still with the other (as they are now), and their own
     *  aliases, which leave the other's (their name's alias is dropped). Patron and pool changes stay. */
    public void undo(String sheetId, Merge merge) {
        jdbc.update("INSERT INTO person (id, sheet_id, name, terms, contact, tags, origin, arrived_on, version,"
                + " list_position) SELECT from_id, sheet_id, name, terms, contact, tags, origin, arrived_on, version,"
                + " (SELECT COALESCE(MAX(list_position), 0) + 1 FROM person WHERE sheet_id = ?) FROM person_merge"
                + " WHERE id = ?", sheetId, merge.id());
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, from_date, grade) SELECT ?, ?, from_date, grade"
                + " FROM person_merge_grade WHERE merge_id = ?", sheetId, merge.fromId(), merge.id());
        jdbc.update("INSERT INTO person_terms (sheet_id, person_id, from_date, terms) SELECT ?, ?, from_date, terms"
                + " FROM person_merge_terms WHERE merge_id = ?", sheetId, merge.fromId(), merge.id());
        jdbc.update("DELETE FROM person_alias WHERE sheet_id = ? AND person_id = ? AND id = (SELECT name_alias_id FROM"
                + " person_merge WHERE id = ?)", sheetId, merge.intoId(), merge.id());
        jdbc.update("UPDATE person_alias SET person_id = ? WHERE sheet_id = ? AND person_id = ? AND id IN"
                + " (SELECT alias_id FROM person_merge_alias WHERE merge_id = ?)", merge.fromId(), sheetId,
                merge.intoId(), merge.id());
        jdbc.update("UPDATE claim SET person_id = ? WHERE sheet_id = ? AND person_id = ? AND id IN"
                + " (SELECT claim_id FROM person_merge_claim WHERE merge_id = ?)", merge.fromId(), sheetId,
                merge.intoId(), merge.id());
        jdbc.update("UPDATE person_merge SET undone = TRUE WHERE id = ?", merge.id());
    }
}
