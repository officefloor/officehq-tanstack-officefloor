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
        List<String> columns = jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE"
                + " table_schema = 'PUBLIC' AND table_name = ? AND is_identity = 'NO' AND is_generated = 'NEVER'"
                + " ORDER BY ordinal_position", String.class, table.toUpperCase(Locale.ROOT));
        List<String> selected = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (String column : columns) {
            String key = column.toLowerCase(Locale.ROOT);
            if (changed.containsKey(key)) {
                selected.add("?");
                values.add(changed.get(key));
            } else {
                selected.add(column);
            }
        }
        values.add(id);
        String sql = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") SELECT "
                + String.join(", ", selected) + " FROM " + table + " WHERE id = ?";
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
}
