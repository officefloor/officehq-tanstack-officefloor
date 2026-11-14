package net.officefloor.hq.app.setting;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RoleRepository {

    private static final RowMapper<Role> ROW = (rs, n) ->
            new Role(rs.getString("sheet_id"), rs.getString("user_name"), rs.getString("role"));

    private final JdbcTemplate jdbc;

    public RoleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The roles assigned on the sheet, by user name. */
    public List<Role> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, user_name, role FROM sheet_role WHERE sheet_id = ? ORDER BY user_name",
                ROW, sheetId);
    }

    /** The user's role on the sheet: clerk when none was assigned. */
    public String roleOf(String sheetId, String user) {
        return jdbc.query("SELECT sheet_id, user_name, role FROM sheet_role WHERE sheet_id = ? AND user_name = ?",
                ROW, sheetId, user).stream().findFirst().map(Role::role).orElse(Role.CLERK);
    }

    /** Assign (or replace) the user's role on the sheet. */
    public Role set(String sheetId, String user, String role, String by) {
        jdbc.update("DELETE FROM sheet_role WHERE sheet_id = ? AND user_name = ?", sheetId, user);
        jdbc.update("INSERT INTO sheet_role (sheet_id, user_name, role, set_by, set_at) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)", sheetId, user, role, by);
        return new Role(sheetId, user, role);
    }
}
