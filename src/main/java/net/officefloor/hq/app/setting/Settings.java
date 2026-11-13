package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * A sheet's settings: its clerk limit (null: no limit), its floor (a claim's live due is at least it), its terms
 * (a claim is due by its date plus this many days), the terms set for each grade that has its own (by grade; any
 * other grade uses the sheet's terms), its late percent (an overdue claim gains it of its figure
 * every full 30 days), its rounding policy date (null: blank, the sheet's
 * today), its period numbering from date (null: off; on or after it, an opened claim is numbered within its period)
 * its cap (null: none; a person's due-total is at most it), its proof threshold (null: none; a claim whose due is above
 * the one in force on its date is settled or approved only once it has a proof) the roles given to user names (any other name is a clerk) and its
 * permission matrix (the roles allowed each of open, settle, void, reassign, close and reopen) and its grace days (null: off;
 * when its today is changed, an open period whose to-date plus them is before the new today is closed).
 * The clerk limit, floor, terms, grade terms, late percent, cap and proof threshold are those in force on the sheet's today; {@code dated}
 * holds every value recorded for them, by from-date.
 */
public record Settings(String sheetId, BigDecimal clerkLimit, BigDecimal floor, int terms, Map<String, Integer> gradeTerms, BigDecimal latePct,
        LocalDate policyDate, LocalDate periodNumberingFrom, BigDecimal cap, BigDecimal proofThreshold, List<UserRole> roles, DatedSettings dated, Permissions permissions, Integer graceDays) {

    /** The role of the user name: the one given to it, else clerk. */
    public Role roleOf(String user) {
        return roles.stream().filter((r) -> r.user().equals(user)).map(UserRole::role).findFirst().orElse(Role.CLERK);
    }

    /** Whether the user's role is allowed the action (open, settle, void, reassign, close or reopen) by the permission matrix. */
    public boolean may(String user, String action) {
        return permissions.allows(action, roleOf(user));
    }

    /** Whether the user may settle a claim with this due (null: n/a, within any limit). */
    public boolean maySettle(String user, BigDecimal due) {
        return roleOf(user) == Role.STEWARD || clerkLimit == null || due == null || due.compareTo(clerkLimit) <= 0;
    }
}
