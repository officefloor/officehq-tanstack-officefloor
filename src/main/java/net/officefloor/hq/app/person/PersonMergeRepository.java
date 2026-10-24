package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The merges made on each sheet, kept so the last one into a person can be undone: the merged person's own rows as
 *  they were, and the claims and aliases the merge moved to the person merged into. */
@Repository
public class PersonMergeRepository {

    /** The merged person's own rows a merge removes, the person last (restored first). */
    private static final List<String> TABLES = List.of("person", "person_grade", "person_grade_log", "person_hold",
            "person_flag", "person_approver", "person_statement", "person_terms", "person_retire_log",
            "person_retirement");

    /** A merge not undone: its id, the merged person (id and name), the person merged into, who made it and on which
     *  sheet-day, the alias the merged person's name became, and the statements the other had then. */
    public record Made(long id, String sheetId, long fromId, String fromName, long intoId, String madeBy,
            LocalDate madeOn, Long nameAliasId, int intoStatements) {
    }

    private final JdbcTemplate jdbc;

    public PersonMergeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Before the merge is made: keep the merged person's rows, and which of their claims and aliases it moves. */
    public long record(Person from, Person into, String user) {
        String sheetId = from.sheetId();
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(c -> {
            var ps = c.prepareStatement("INSERT INTO person_merge (sheet_id, from_id, from_name, into_id, made_by, "
                    + "made_on, into_statements) VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?), ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, from.id());
            ps.setString(3, from.name());
            ps.setLong(4, into.id());
            ps.setString(5, user);
            ps.setString(6, sheetId);
            ps.setInt(7, into.statementCount());
            return ps;
        }, key);
        long id = key.getKey().longValue();
        jdbc.update("INSERT INTO person_merge_claim (merge_id, claim_id) SELECT ?, id FROM claim "
                + "WHERE sheet_id = ? AND person_id = ?", id, sheetId, from.id());
        jdbc.update("INSERT INTO person_merge_alias (merge_id, alias_id) SELECT ?, id FROM person_alias "
                + "WHERE sheet_id = ? AND person_id = ?", id, sheetId, from.id());
        for (String table : TABLES) {
            String where = "person".equals(table) ? "id" : "person_id";
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT * FROM " + table + " WHERE sheet_id = ? AND " + where + " = ?", sheetId, from.id());
            List<String> generated = generated(table);
            for (int n = 0; n < rows.size(); n++) {
                for (var col : rows.get(n).entrySet()) {
                    if (generated.contains(col.getKey().toUpperCase())) {
                        continue;
                    }
                    jdbc.update("INSERT INTO person_merge_row (merge_id, table_name, row_no, column_name, val) "
                            + "VALUES (?, ?, ?, ?, ?)", id, table, n, col.getKey(),
                            col.getValue() == null ? null : col.getValue().toString());
                }
            }
        }
        return id;
    }

    /** After the merge is made: the alias the merged person's name became. */
    public void named(long mergeId, Person into, String name) {
        jdbc.update("UPDATE person_merge SET name_alias_id = (SELECT MAX(id) FROM person_alias WHERE sheet_id = ? "
                + "AND person_id = ? AND alias = ?) WHERE id = ?", into.sheetId(), into.id(), name.trim(), mergeId);
    }

    /** The last merge into the person not undone, or null when there is none. */
    public Made last(String sheetId, long intoId) {
        return jdbc.query("SELECT * FROM person_merge WHERE sheet_id = ? AND into_id = ? AND undone = FALSE "
                + "ORDER BY id DESC LIMIT 1", (rs, i) -> new Made(rs.getLong("id"), rs.getString("sheet_id"),
                        rs.getLong("from_id"), rs.getString("from_name"), rs.getLong("into_id"),
                        rs.getString("made_by"), rs.getObject("made_on", LocalDate.class),
                        (Long) rs.getObject("name_alias_id"), rs.getInt("into_statements")), sheetId, intoId)
                .stream().findFirst().orElse(null);
    }

    /** The claims the merge moved to the person merged into. */
    public List<Long> claims(long mergeId) {
        return jdbc.queryForList("SELECT claim_id FROM person_merge_claim WHERE merge_id = ?", Long.class, mergeId);
    }

    /** Put the merged person's own rows back as they were before the merge, the person at the end of the list; any
     *  of their statements the person merged into took are given back. */
    public void restoreRows(Made merge) {
        for (String table : TABLES) {
            Map<Integer, Map<String, String>> rows = new TreeMap<>();
            jdbc.query("SELECT row_no, column_name, val FROM person_merge_row WHERE merge_id = ? AND table_name = ?",
                    rs -> {
                        rows.computeIfAbsent(rs.getInt(1), k -> new LinkedHashMap<>()).put(rs.getString(2),
                                rs.getString(3));
                    }, merge.id(), table);
            for (Map<String, String> row : rows.values()) {
                if (!"person".equals(table) && row.get("ID") != null) {
                    jdbc.update("DELETE FROM " + table + " WHERE id = ?", row.get("ID"));
                }
                List<String> cols = new ArrayList<>(row.keySet());
                List<Object> vals = new ArrayList<>(row.values());
                jdbc.update("INSERT INTO " + table + " (" + String.join(", ", cols) + ") VALUES ("
                        + String.join(", ", cols.stream().map(c -> "?").toList()) + ")", vals.toArray());
            }
        }
        jdbc.update("UPDATE person SET list_position = (SELECT COALESCE(MAX(p.list_position), 0) + 1 FROM person p "
                + "WHERE p.sheet_id = ?) WHERE sheet_id = ? AND id = ?", merge.sheetId(), merge.sheetId(),
                merge.fromId());
    }

    /** Give the merged person back their own aliases still with the person merged into, and drop the alias their name
     *  became there. */
    public void restoreAliases(Made merge) {
        jdbc.update("UPDATE person_alias SET person_id = ? WHERE sheet_id = ? AND person_id = ? AND id IN "
                + "(SELECT alias_id FROM person_merge_alias WHERE merge_id = ?)", merge.fromId(), merge.sheetId(),
                merge.intoId(), merge.id());
        if (merge.nameAliasId() != null) {
            jdbc.update("DELETE FROM person_alias WHERE sheet_id = ? AND person_id = ? AND id = ?", merge.sheetId(),
                    merge.intoId(), merge.nameAliasId());
        }
    }

    /** Take the merged person's statements back off the other's count; when the other had none before the merge and
     *  has taken none since, their last statement goes with them. */
    public void restoreStatements(Made merge, Person from, Person into) {
        int count = Math.max(into.statementCount() - from.statementCount(), merge.intoStatements());
        if (merge.intoStatements() == 0 && count == 0) {
            jdbc.update("UPDATE person SET statement_count = 0, stated_total = NULL, statement_date = NULL "
                    + "WHERE sheet_id = ? AND id = ?", into.sheetId(), into.id());
        } else {
            jdbc.update("UPDATE person SET statement_count = ? WHERE sheet_id = ? AND id = ?", count, into.sheetId(),
                    into.id());
        }
    }

    public void undone(Made merge) {
        jdbc.update("UPDATE person_merge SET undone = TRUE WHERE id = ?", merge.id());
    }

    /** The table's generated columns (upper case), which are never written. */
    private List<String> generated(String table) {
        return jdbc.queryForList("SELECT UPPER(COLUMN_NAME) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? "
                + "AND IS_GENERATED = 'ALWAYS'", String.class, table.toUpperCase());
    }
}
