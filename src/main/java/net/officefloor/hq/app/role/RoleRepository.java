package net.officefloor.hq.app.role;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored roles (clerk, steward or viewer), one per user name per sheet. A name with no row is a clerk. */
@Repository
public class RoleRepository {

    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";
    public static final String VIEWER = "viewer";

    private final JdbcTemplate jdbc;
    private final DelegationRepository delegations;

    public RoleRepository(JdbcTemplate jdbc, DelegationRepository delegations) {
        this.jdbc = jdbc;
        this.delegations = delegations;
    }

    /** The roles given on the sheet, by user name. */
    public List<Role> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, user_name, role FROM user_role WHERE sheet_id = ? ORDER BY user_name",
                RoleRepository::map, sheetId);
    }

    /** The role the user acts in on the sheet, for every rule: a steward while a delegation to the user holds the
     *  sheet's today, otherwise the role given. */
    public String roleOf(String sheetId, String user) {
        String given = givenRoleOf(sheetId, user);
        if (!STEWARD.equals(given) && delegations.actingFor(sheetId, user).isPresent()) {
            return STEWARD;
        }
        return given;
    }

    /** The user's role given on the sheet: clerk unless given another (a delegation not counted). */
    public String givenRoleOf(String sheetId, String user) {
        List<String> found = jdbc.queryForList("SELECT role FROM user_role WHERE sheet_id = ? AND user_name = ?",
                String.class, sheetId, user);
        return found.isEmpty() ? CLERK : found.get(0);
    }

    /** Stores the role, replacing the one the user had. */
    public Role save(Role role) {
        jdbc.update("DELETE FROM user_role WHERE sheet_id = ? AND user_name = ?", role.sheetId(), role.user());
        jdbc.update("INSERT INTO user_role (sheet_id, user_name, role) VALUES (?, ?, ?)",
                role.sheetId(), role.user(), role.role());
        return role;
    }

    private static Role map(ResultSet rs, int n) throws SQLException {
        return new Role(rs.getString("sheet_id"), rs.getString("user_name"), rs.getString("role"));
    }
}
