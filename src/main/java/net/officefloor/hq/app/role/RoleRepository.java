package net.officefloor.hq.app.role;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The roles each sheet assigns, by user name. A name without one is a clerk. */
@Repository
public class RoleRepository {

    private final JdbcTemplate jdbc;

    public RoleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Role> findBySheet(String sheetId) {
        return jdbc.query("SELECT * FROM sheet_role WHERE sheet_id = ? ORDER BY user_name",
                (rs, i) -> new Role(rs.getString("sheet_id"), rs.getString("user_name"), rs.getString("role")), sheetId);
    }

    /** The user's role on the sheet: clerk unless assigned another. */
    public String roleOf(String sheetId, String user) {
        List<String> found = jdbc.queryForList("SELECT role FROM sheet_role WHERE sheet_id = ? AND user_name = ?",
                String.class, sheetId, user);
        return found.isEmpty() ? Role.CLERK : found.get(0);
    }

    public void save(Role role) {
        jdbc.update("MERGE INTO sheet_role (sheet_id, user_name, role) KEY (sheet_id, user_name) VALUES (?, ?, ?)",
                role.sheetId(), role.user(), role.role());
    }
}
