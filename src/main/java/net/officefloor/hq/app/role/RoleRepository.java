package net.officefloor.hq.app.role;

import java.util.List;
import net.officefloor.hq.app.delegation.DelegationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The roles store: at most one role per user name, kept per sheet. */
@Repository
public class RoleRepository {

    private static final RowMapper<Role> ROW = (rs, n) -> new Role(rs.getString("sheet_id"),
            rs.getString("user_name"), rs.getString("role"));

    private final JdbcTemplate jdbc;
    private final DelegationRepository delegations;

    public RoleRepository(JdbcTemplate jdbc, DelegationRepository delegations) {
        this.jdbc = jdbc;
        this.delegations = delegations;
    }

    /** The sheet's roles, by user name. */
    public List<Role> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, user_name, role FROM sheet_role WHERE sheet_id = ?"
                + " ORDER BY user_name", ROW, sheetId);
    }

    /**
     * The role the user acts in on the sheet, in every rule: steward while the steward role is delegated to
     * them for the sheet's today, otherwise the role assigned.
     */
    public String roleOf(String sheetId, String user) {
        if (delegations.delegatorOf(sheetId, user) != null) return Role.STEWARD;
        return assignedRoleOf(sheetId, user);
    }

    /** The user's own assigned role on the sheet, leaving out any delegation: clerk when none was assigned. */
    public String assignedRoleOf(String sheetId, String user) {
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
