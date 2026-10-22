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

    /** The user's role on the sheet: the one assigned, else clerk. */
    public String of(String sheetId, String user) {
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
