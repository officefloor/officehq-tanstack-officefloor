package net.officefloor.hq.app.role;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored roles (clerk, steward, viewer or restricted), one per user name per sheet. A name with no row is a clerk. */
@Repository
public class RoleRepository {

    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";
    public static final String VIEWER = "viewer";
    /** Sees on the people list only their own crews' people and themselves; otherwise as a clerk. */
    public static final String RESTRICTED = "restricted";

    private final JdbcTemplate jdbc;
    private final DelegationRepository delegations;
    private final SheetRepository sheets;

    public RoleRepository(JdbcTemplate jdbc, DelegationRepository delegations, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.delegations = delegations;
        this.sheets = sheets;
    }

    /** The roles given on the sheet, by user name. */
    public List<Role> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, user_name, role FROM user_role WHERE sheet_id = ? ORDER BY user_name",
                RoleRepository::map, sheetId);
    }

    /** The roles in force on the sheet at the end of the date, by user name: for each name given a role, the role given
     *  as of the date (see givenRoleOfAsOf); a name first given one after the date is left out. */
    public List<Role> listAsOf(String sheetId, LocalDate asOf) {
        List<String> users = jdbc.queryForList("SELECT user_name FROM user_role WHERE sheet_id = ?"
                + " UNION SELECT user_name FROM user_role_log WHERE sheet_id = ? ORDER BY user_name",
                String.class, sheetId, sheetId);
        List<Role> inForce = new ArrayList<>();
        for (String user : users) {
            Integer logged = jdbc.queryForObject("SELECT COUNT(*) FROM user_role_log WHERE sheet_id = ?"
                    + " AND user_name = ?", Integer.class, sheetId, user);
            Integer counted = jdbc.queryForObject("SELECT COUNT(*) FROM user_role_log WHERE sheet_id = ?"
                    + " AND user_name = ? AND set_on <= ?", Integer.class, sheetId, user, asOf);
            if (logged == null || logged == 0 || (counted != null && counted > 0)) {
                inForce.add(new Role(sheetId, user, givenRoleOfAsOf(sheetId, user, asOf)));
            }
        }
        return inForce;
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

    /** The role the user acted in on the sheet at the end of the date: a steward while a delegation to the user, already
     *  made then, held the date, otherwise the role given as of the date. */
    public String roleOfAsOf(String sheetId, String user, LocalDate asOf) {
        String given = givenRoleOfAsOf(sheetId, user, asOf);
        if (!STEWARD.equals(given) && delegations.actingForAsOf(sheetId, user, asOf).isPresent()) {
            return STEWARD;
        }
        return given;
    }

    /** The user's role given on the sheet as of the date: the last one given while the sheet's today was on or before
     *  it (one given before that was kept counting from the beginning); clerk when none was. */
    public String givenRoleOfAsOf(String sheetId, String user, LocalDate asOf) {
        Integer logged = jdbc.queryForObject("SELECT COUNT(*) FROM user_role_log WHERE sheet_id = ? AND user_name = ?",
                Integer.class, sheetId, user);
        if (logged == null || logged == 0) {
            return givenRoleOf(sheetId, user);
        }
        List<String> found = jdbc.queryForList("SELECT role FROM user_role_log WHERE sheet_id = ? AND user_name = ?"
                + " AND set_on <= ? ORDER BY id DESC", String.class, sheetId, user, asOf);
        return found.isEmpty() ? CLERK : found.get(0);
    }

    /** Stores the role, replacing the one the user had, and logs it with the sheet's today. */
    public Role save(Role role) {
        jdbc.update("INSERT INTO user_role_log (sheet_id, user_name, role, set_on) VALUES (?, ?, ?, ?)",
                role.sheetId(), role.user(), role.role(), sheets.today(role.sheetId()));
        jdbc.update("DELETE FROM user_role WHERE sheet_id = ? AND user_name = ?", role.sheetId(), role.user());
        jdbc.update("INSERT INTO user_role (sheet_id, user_name, role) VALUES (?, ?, ?)",
                role.sheetId(), role.user(), role.role());
        return role;
    }

    private static Role map(ResultSet rs, int n) throws SQLException {
        return new Role(rs.getString("sheet_id"), rs.getString("user_name"), rs.getString("role"));
    }
}
