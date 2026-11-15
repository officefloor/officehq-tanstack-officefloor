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

    /** The role the user acts in on the sheet: steward while a delegation to them covers the sheet's
     *  today, else the one assigned (clerk when none was). */
    public String roleOf(String sheetId, String user) {
        return delegatorOf(sheetId, user) != null ? Role.STEWARD : assignedRoleOf(sheetId, user);
    }

    /** The user's own role on the sheet, delegations aside: clerk when none was assigned. */
    public String assignedRoleOf(String sheetId, String user) {
        return jdbc.query("SELECT sheet_id, user_name, role FROM sheet_role WHERE sheet_id = ? AND user_name = ?",
                ROW, sheetId, user).stream().findFirst().map(Role::role).orElse(Role.CLERK);
    }

    /** The steward who delegated the steward role to the user by a delegation covering the sheet's
     *  today (the latest, when several do); null when none does. */
    public String delegatorOf(String sheetId, String user) {
        return jdbc.queryForList("SELECT d.delegated_by FROM delegation d JOIN sheet s ON s.id = d.sheet_id "
                + "WHERE d.sheet_id = ? AND d.user_name = ? AND d.from_date <= s.today AND s.today <= d.to_date "
                + "ORDER BY d.id DESC LIMIT 1", String.class, sheetId, user).stream().findFirst().orElse(null);
    }

    /** Assign (or replace) the user's role on the sheet. */
    public Role set(String sheetId, String user, String role, String by) {
        jdbc.update("DELETE FROM sheet_role WHERE sheet_id = ? AND user_name = ?", sheetId, user);
        jdbc.update("INSERT INTO sheet_role (sheet_id, user_name, role, set_by, set_at) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)", sheetId, user, role, by);
        return new Role(sheetId, user, role);
    }
}
