package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.List;

/**
 * A sheet's settings: its clerk limit (null: no limit), its floor (a claim's live due is at least it), its terms
 * (a claim is due by its date plus this many days), its late percent (an overdue claim gains it of its figure
 * every full 30 days) and the
 * roles given to user names (any other name is a clerk).
 */
public record Settings(String sheetId, BigDecimal clerkLimit, BigDecimal floor, int terms, BigDecimal latePct,
        List<UserRole> roles) {

    /** The role of the user name: the one given to it, else clerk. */
    public Role roleOf(String user) {
        return roles.stream().filter((r) -> r.user().equals(user)).map(UserRole::role).findFirst().orElse(Role.CLERK);
    }

    /** Whether the user may settle a claim with this due (null: n/a, within any limit). */
    public boolean maySettle(String user, BigDecimal due) {
        return roleOf(user) == Role.STEWARD || clerkLimit == null || due == null || due.compareTo(clerkLimit) <= 0;
    }
}
