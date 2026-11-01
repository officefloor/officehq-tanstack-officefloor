package net.officefloor.hq.app.role;

import java.util.List;
import net.officefloor.hq.app.delegation.Delegations;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The roles given per sheet, by user name; a name given none is a clerk. A user lent the steward role acts as one
 *  while the sheet's today is inside the delegation. */
@Repository
public class Roles {

    private static final RowMapper<Role> ROW = (rs, i) -> new Role(rs.getString("user_name"), rs.getString("role"));

    private final JdbcTemplate jdbc;
    private final Delegations delegations;
    private final SheetClock clock;

    public Roles(JdbcTemplate jdbc, Delegations delegations, SheetClock clock) {
        this.jdbc = jdbc;
        this.delegations = delegations;
        this.clock = clock;
    }

    /** Every role given on the sheet, by user name. */
    public List<Role> list(String sheet) {
        return jdbc.query("SELECT user_name, role FROM user_role WHERE sheet = ? ORDER BY user_name", ROW, sheet);
    }

    /** The role the user acts in on the sheet: steward while lent the role, else the role given. */
    public String of(String sheet, String user) {
        if (delegations.delegatorOn(sheet, user, clock.today(sheet)).isPresent()) {
            return Role.STEWARD;
        }
        return given(sheet, user);
    }

    /** The user's role given on the sheet: clerk unless given another. */
    public String given(String sheet, String user) {
        List<String> found = jdbc.queryForList("SELECT role FROM user_role WHERE sheet = ? AND user_name = ?",
                String.class, sheet, user);
        return found.isEmpty() ? Role.CLERK : found.get(0);
    }

    public void set(String sheet, Role role) {
        jdbc.update("MERGE INTO user_role (sheet, user_name, role) KEY (sheet, user_name) VALUES (?, ?, ?)", sheet,
                role.user(), role.role());
    }
}
