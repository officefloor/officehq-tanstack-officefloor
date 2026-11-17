package net.officefloor.hq.app.person;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Keeps every merge of a person into another (see PersonMerger#merge) and undoes the last one into a person (see
 *  #undo). */
@Repository
public class MergeUndoer {

    /** The merged person's own rows a merge removes, kept (in merge_saved_<table>) to be put back by an undo; the person
     *  first, as the others refer to them. */
    private static final List<String> SAVED = List.of("person", "person_hold", "watch", "person_approver", "person_grade",
            "person_grade_log", "person_retire_log", "person_retirement", "person_terms_log", "person_terms_from");

    /** The last merge into a person: its id, the person merged, who merged them, the sheet's today it was made on, and
     *  whether it has been undone. */
    public record LastMerge(long id, long fromId, String by, LocalDate madeOn, boolean undone) {
    }

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;
    private final PersonRepository people;

    public MergeUndoer(JdbcTemplate jdbc, SheetRepository sheets, PersonRepository people) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.people = people;
    }

    /** Keeps the merge of {@code fromId} into {@code intoId} about to be made: the claims and aliases that move, and the
     *  merged person's own rows as they stand. Returns the merge's id. */
    public long keep(String sheetId, long fromId, long intoId, String by, List<Long> claims) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO person_merge (sheet_id, from_id, into_id, by_user, made_on)"
                    + " VALUES (?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, fromId);
            ps.setLong(3, intoId);
            ps.setString(4, by);
            ps.setDate(5, Date.valueOf(sheets.today(sheetId)));
            return ps;
        }, keys);
        long id = keys.getKey().longValue();
        for (Long claim : claims) {
            jdbc.update("INSERT INTO person_merge_claim (merge_id, claim_id) VALUES (?, ?)", id, claim);
        }
        jdbc.update("INSERT INTO person_merge_alias (merge_id, alias_id) SELECT ?, id FROM person_alias"
                + " WHERE sheet_id = ? AND person_id = ?", id, sheetId, fromId);
        for (String table : SAVED) {
            String columns = columns(table);
            jdbc.update("INSERT INTO merge_saved_" + table + " (merge_id, " + columns + ") SELECT ?, " + columns + " FROM "
                    + table + " WHERE sheet_id = ? AND " + (table.equals("person") ? "id" : "person_id") + " = ?", id,
                    sheetId, fromId);
        }
        return id;
    }

    /** Keeps the alias the merged person's name became on the person merged into. */
    public void keepNameAlias(long mergeId, String sheetId, long intoId, String name) {
        jdbc.update("UPDATE person_merge SET name_alias_id = (SELECT MAX(id) FROM person_alias WHERE sheet_id = ?"
                + " AND person_id = ? AND alias = ?) WHERE id = ?", sheetId, intoId, name, mergeId);
    }

    /** The last merge into the person, or null when none was ever made. */
    public LastMerge last(String sheetId, long intoId) {
        List<LastMerge> found = jdbc.query("SELECT id, from_id, by_user, made_on, undone FROM person_merge"
                + " WHERE sheet_id = ? AND into_id = ? ORDER BY id DESC LIMIT 1",
                (rs, n) -> new LastMerge(rs.getLong("id"), rs.getLong("from_id"), rs.getString("by_user"),
                        rs.getDate("made_on").toLocalDate(), rs.getBoolean("undone")), sheetId, intoId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The name the merged person had, as kept by the merge. */
    public String fromName(long mergeId) {
        return jdbc.queryForObject("SELECT name FROM merge_saved_person WHERE merge_id = ?", String.class, mergeId);
    }

    /** Whether a person on the sheet now has the name, case ignored. */
    public boolean nameHeld(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(?)",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /**
     * Undoes the merge into {@code intoId}: the merged person is back, as they stood before it, at the end of the list;
     * the claims that came from them and are still the other's go back to them as they are now; their own aliases leave
     * the other's and are theirs again, and the alias their name became is dropped. Everything else the merge did
     * (patrons, pools, statements, period totals, crews) stays. Returns the person back.
     */
    public Person undo(String sheetId, long intoId, LastMerge merge) {
        long id = merge.id();
        for (String table : SAVED) {
            String columns = columns(table);
            jdbc.update("INSERT INTO " + table + " (" + columns + ") SELECT " + columns + " FROM merge_saved_" + table
                    + " WHERE merge_id = ?", id);
            jdbc.update("DELETE FROM merge_saved_" + table + " WHERE merge_id = ?", id);
        }
        people.toEnd(sheetId, merge.fromId());
        jdbc.update("DELETE FROM person_merged WHERE sheet_id = ? AND id = ?", sheetId, merge.fromId());
        jdbc.update("UPDATE claim SET person_id = ? WHERE sheet_id = ? AND person_id = ? AND id IN"
                + " (SELECT claim_id FROM person_merge_claim WHERE merge_id = ?)", merge.fromId(), sheetId, intoId, id);
        jdbc.update("UPDATE person_alias SET person_id = ? WHERE sheet_id = ? AND person_id = ? AND id IN"
                + " (SELECT alias_id FROM person_merge_alias WHERE merge_id = ?)", merge.fromId(), sheetId, intoId, id);
        jdbc.update("DELETE FROM person_alias WHERE sheet_id = ? AND person_id = ? AND id = (SELECT name_alias_id FROM"
                + " person_merge WHERE id = ?)", sheetId, intoId, id);
        jdbc.update("UPDATE person_merge SET undone = TRUE WHERE id = ?", id);
        return people.find(sheetId, merge.fromId());
    }

    /** The table's own (not generated) columns, comma separated. */
    private String columns(String table) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                + " WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ? AND IS_GENERATED = 'NEVER' ORDER BY ORDINAL_POSITION",
                table.toUpperCase(Locale.ROOT));
        return rows.stream().map((r) -> (String) r.get("COLUMN_NAME")).collect(Collectors.joining(", "));
    }
}
