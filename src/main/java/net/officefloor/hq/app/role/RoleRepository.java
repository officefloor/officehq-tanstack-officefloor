package net.officefloor.hq.app.role;

import java.sql.Date;
import java.time.LocalDate;
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
     * The sheet's roles as they stood at the end of {@code day}, by user name: for each user, the last role
     * assigned to them on or before it. A user first assigned a role after it is left out.
     */
    public List<Role> findAsOf(String sheetId, LocalDate day) {
        return jdbc.query("SELECT h.sheet_id, h.user_name, h.role FROM role_history h WHERE h.sheet_id = ?"
                + " AND h.id = (SELECT MAX(l.id) FROM role_history l WHERE l.sheet_id = h.sheet_id"
                + " AND l.user_name = h.user_name AND l.set_on <= ?) ORDER BY h.user_name", ROW,
                sheetId, Date.valueOf(day));
    }

    /**
     * The role the user acts in on the sheet, in every rule: steward while the steward role is delegated to
     * them for the sheet's today, otherwise the role assigned.
     */
    public String roleOf(String sheetId, String user) {
        if (delegations.delegatorOf(sheetId, user) != null) return Role.STEWARD;
        return assignedRoleOf(sheetId, user);
    }

    /**
     * The role the user acted in on the sheet as it stood at the end of {@code day}: steward while the
     * steward role was delegated to them for it, otherwise the last role assigned to them by then.
     */
    public String roleOf(String sheetId, String user, LocalDate day) {
        if (delegations.delegatedOn(sheetId, user, day)) return Role.STEWARD;
        List<String> found = jdbc.queryForList("SELECT role FROM role_history WHERE sheet_id = ? AND user_name = ?"
                + " AND set_on <= ? ORDER BY id DESC LIMIT 1", String.class, sheetId, user, Date.valueOf(day));
        return found.isEmpty() ? Role.CLERK : found.get(0);
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
        jdbc.update("INSERT INTO role_history (sheet_id, user_name, role, set_on) SELECT ?, ?, ?, today FROM sheet"
                + " WHERE id = ?", role.sheetId(), role.user(), role.role(), role.sheetId());
    }
}
