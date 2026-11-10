package net.officefloor.hq.app.role;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The roles store: at most one role per user name, kept per sheet. */
@Repository
public class RoleRepository {

    private static final RowMapper<Role> ROW = (rs, n) -> new Role(rs.getString("sheet_id"),
            rs.getString("user_name"), rs.getString("role"));

    private final JdbcTemplate jdbc;

    public RoleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's roles, by user name. */
    public List<Role> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, user_name, role FROM sheet_role WHERE sheet_id = ?"
                + " ORDER BY user_name", ROW, sheetId);
    }

    /** The user's role on the sheet: clerk when none was assigned. */
    public String roleOf(String sheetId, String user) {
        List<String> found = jdbc.queryForList(
                "SELECT role FROM sheet_role WHERE sheet_id = ? AND user_name = ?", String.class, sheetId, user);
        return found.isEmpty() ? Role.CLERK : found.get(0);
    }

    /** Assign the user's role, replacing any they had. */
    public void save(Role role) {
        jdbc.update("DELETE FROM sheet_role WHERE sheet_id = ? AND user_name = ?", role.sheetId(), role.user());
        jdbc.update("INSERT INTO sheet_role (sheet_id, user_name, role) VALUES (?, ?, ?)",
                role.sheetId(), role.user(), role.role());
    }
}
