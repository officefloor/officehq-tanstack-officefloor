package net.officefloor.hq.app.access;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The roles assigned on each sheet; a user name without one is a clerk. */
@Repository
public class RoleRepository {

    private static final RowMapper<Role> ROW = (rs, n) -> new Role(rs.getString("sheet_id"),
            rs.getString("user_name"), rs.getString("role"));

    private final JdbcTemplate jdbc;

    public RoleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's assigned roles, by user name. */
    public List<Role> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, user_name, role FROM sheet_role WHERE sheet_id = ? ORDER BY user_name", ROW,
                sheetId);
    }

    /** The user's role on the sheet: clerk unless assigned another. */
    public String roleOf(String sheetId, String user) {
        return jdbc.query("SELECT sheet_id, user_name, role FROM sheet_role WHERE sheet_id = ? AND user_name = ?", ROW,
                sheetId, user).stream().findFirst().map(Role::role).orElse(Role.CLERK);
    }

    /** Assign the user a role, replacing the one assigned before. */
    public Role set(String sheetId, String user, String role) {
        jdbc.update("MERGE INTO sheet_role (sheet_id, user_name, role) KEY (sheet_id, user_name) VALUES (?, ?, ?)",
                sheetId, user, role);
        return new Role(sheetId, user, role);
    }

    /** Whether the user is a viewer on the sheet, who may change nothing. */
    public boolean isViewer(String sheetId, String user) {
        return Role.VIEWER.equals(roleOf(sheetId, user));
    }
}
