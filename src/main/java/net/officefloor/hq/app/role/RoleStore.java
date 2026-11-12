package net.officefloor.hq.app.role;

import java.util.List;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The roles assigned per sheet. A user name with none assigned is a clerk; one with a delegation of the
 *  steward role in force on the sheet's today acts as a steward (see {@link DelegationStore}). */
@Repository
public class RoleStore {

    private final JdbcTemplate jdbc;
    private final DelegationStore delegations;

    public RoleStore(JdbcTemplate jdbc, DelegationStore delegations) {
        this.jdbc = jdbc;
        this.delegations = delegations;
    }

    /** Every role assigned on the sheet, by user name. */
    public List<RoleResponse> list(String sheetId) {
        return jdbc.query("SELECT user_name, role FROM sheet_role WHERE sheet_id = ? ORDER BY user_name",
                (rs, i) -> new RoleResponse(rs.getString("user_name"), Role.valueOf(rs.getString("role")).shown()),
                sheetId);
    }

    /** The role the user acts in on the sheet: steward while a delegation to them is in force, else
     *  the role assigned. */
    public Role find(String sheetId, String user) {
        return delegations.delegator(sheetId, user) != null ? Role.STEWARD : assigned(sheetId, user);
    }

    /** The user's role as assigned on the sheet, delegations aside: clerk unless assigned otherwise. */
    public Role assigned(String sheetId, String user) {
        List<String> found = jdbc.queryForList("SELECT role FROM sheet_role WHERE sheet_id = ? AND user_name = ?",
                String.class, sheetId, user);
        return found.isEmpty() ? Role.CLERK : Role.valueOf(found.get(0));
    }

    /** Refuses any change by a viewer: a viewer may change nothing on the sheet. */
    public void refuseViewer(String sheetId, String user) throws InvalidSheetException {
        if (find(sheetId, user == null ? "" : user.trim()) == Role.VIEWER) {
            throw new InvalidSheetException("A viewer may change nothing");
        }
    }

    public void set(String sheetId, String user, Role role) {
        jdbc.update("MERGE INTO sheet_role (sheet_id, user_name, role) KEY (sheet_id, user_name) VALUES (?, ?, ?)",
                sheetId, user, role.name());
    }
}
