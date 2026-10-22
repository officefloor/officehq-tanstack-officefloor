package net.officefloor.hq.app.role;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Roles by sheet: at most one per user name; a name with none is a clerk. */
@Repository
public class RoleRepository {

    private static final RowMapper<Role> ROW = (rs, i) -> new Role(rs.getString("sheet_id"),
            rs.getString("user_name"), rs.getString("role"));

    private final JdbcTemplate jdbc;

    public RoleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's assigned roles, by user name. */
    public List<Role> list(String sheetId) {
        return jdbc.query("SELECT * FROM sheet_role WHERE sheet_id = ? ORDER BY user_name", ROW, sheetId);
    }

    /** The role the user acts in on the sheet: steward while a delegation to them is in force by the sheet's
     *  today, else the one assigned. */
    public String of(String sheetId, String user) {
        Integer delegated = jdbc.queryForObject("SELECT COUNT(*) FROM sheet_delegation d JOIN sheet s "
                + "ON s.id = d.sheet_id WHERE d.sheet_id = ? AND d.user_name = ? "
                + "AND s.today BETWEEN d.from_date AND d.to_date", Integer.class, sheetId, user);
        return delegated != null && delegated > 0 ? Role.STEWARD : assigned(sheetId, user);
    }

    /** The user's assigned role on the sheet: the one assigned, else clerk. */
    public String assigned(String sheetId, String user) {
        List<String> found = jdbc.queryForList("SELECT role FROM sheet_role WHERE sheet_id = ? AND user_name = ?",
                String.class, sheetId, user);
        return found.isEmpty() ? Role.CLERK : found.get(0);
    }

    /** Assign the user's role on the sheet, replacing any they had. */
    public Role set(String sheetId, String user, String role) {
        jdbc.update("MERGE INTO sheet_role KEY (sheet_id, user_name) VALUES (?, ?, ?)", sheetId, user, role);
        return new Role(sheetId, user, role);
    }
}
