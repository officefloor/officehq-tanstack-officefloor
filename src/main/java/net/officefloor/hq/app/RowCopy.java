package net.officefloor.hq.app;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/** Copies a stored row to a new row of the same table under a new id, every column kept but those given anew. */
public final class RowCopy {

    private RowCopy() {
    }

    /**
     * Copy the row with this id of the table (its id the identity column {@code id}), giving the columns named in
     * {@code changed} (lower case) their values there instead; the new row's id.
     */
    public static long copy(JdbcTemplate jdbc, String table, long id, Map<String, Object> changed) {
        List<String> columns = columns(jdbc, table);
        List<Object> values = new ArrayList<>();
        String sql = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") SELECT "
                + selected(columns, changed, values) + " FROM " + table + " WHERE id = ?";
        values.add(id);
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[] { "id" });
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /**
     * Copy every row of the table matching {@code where} (its parameters {@code args}) to new rows of the same table,
     * in the order {@code orderBy} gives (identity columns numbered afresh in that order), giving the columns named in
     * {@code changed} (lower case) their values there instead; how many were copied.
     */
    public static int copyAll(JdbcTemplate jdbc, String table, String where, List<Object> args, String orderBy,
            Map<String, Object> changed) {
        List<String> columns = columns(jdbc, table);
        List<Object> values = new ArrayList<>();
        String sql = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") SELECT "
                + selected(columns, changed, values) + " FROM " + table + " WHERE " + where
                + (orderBy == null ? "" : " ORDER BY " + orderBy);
        values.addAll(args);
        return jdbc.update(sql, values.toArray());
    }

    /** The table's columns a copy writes: all but its identity and generated ones, in their order. */
    private static List<String> columns(JdbcTemplate jdbc, String table) {
        return jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE"
                + " table_schema = 'PUBLIC' AND table_name = ? AND is_identity = 'NO' AND is_generated = 'NEVER'"
                + " ORDER BY ordinal_position", String.class, table.toUpperCase(Locale.ROOT));
    }

    /** The select list copying the columns, a parameter for each changed one (its value added to {@code values}). */
    private static String selected(List<String> columns, Map<String, Object> changed, List<Object> values) {
        List<String> selected = new ArrayList<>();
        for (String column : columns) {
            String key = column.toLowerCase(Locale.ROOT);
            if (changed.containsKey(key)) {
                selected.add("?");
                values.add(changed.get(key));
            } else {
                selected.add(column);
            }
        }
        return String.join(", ", selected);
    }
}
