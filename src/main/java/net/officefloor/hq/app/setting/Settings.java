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
 * its entry date (null: off; a new claim left without a date takes it instead of the sheet's today)
 * its cap (null: none; a person's due-total is at most it), its proof threshold (null: none; a claim whose due is above
 * the one in force on its date is settled or approved only once it has a proof) the roles given to user names (any other name is a clerk) and its
 * permission matrix (the roles allowed each of open, settle, void, reassign, close and reopen) and its grace days (null: off;
 * when its today is changed, an open period whose to-date plus them is before the new today is closed).
 * The clerk limit, floor, terms, grade terms, late percent, cap and proof threshold are those in force on the sheet's today; {@code dated}
 * holds every value recorded for them, by from-date. {@code delegations} are the steward role delegated to user names, in the
 * order made; one in force on {@code today} (the sheet's today) makes its user act as a steward in every rule.
 * {@code pot} is the sheet's pot (null: none), the pot of each pool that is a member of no pool, shared out among its members when it is balanced; it is not dated.
 * {@code parent} is the sheet's parent sheet id (null: none), whose rates a grade with none of its own in force takes; it is not dated.
 * {@code fiscalStartMonth} is the month (1 to 12) each of the sheet's fiscal years starts in; it is not dated.
 * {@code escalateAfter} is the sheet's escalation gap in days (null: off): reminding a claim whose previous reminder was at least
 * that many days before today escalates it (once per claim); it is not dated.
 * {@code alertThreshold} is the sheet's alert threshold (null: off), above which a person's due-total crossing is alerted in the audit; it is not dated.
 */
public record Settings(String sheetId, BigDecimal clerkLimit, BigDecimal floor, int terms, Map<String, Integer> gradeTerms, BigDecimal latePct,
        LocalDate policyDate, LocalDate periodNumberingFrom, LocalDate entryDate, BigDecimal cap, BigDecimal proofThreshold, List<UserRole> roles, DatedSettings dated, Permissions permissions, Integer graceDays,
        LocalDate today, List<Delegation> delegations, BigDecimal pot, String parent, int fiscalStartMonth, BigDecimal alertThreshold,
        Integer escalateAfter) {

    /** The role the user name acts in: steward while a delegation to it is in force on today, else its given role. */
    public Role roleOf(String user) {
        return delegatorOf(user) != null ? Role.STEWARD : givenRoleOf(user);
    }

    /** The role given to the user name, else clerk (a delegation aside). */
    public Role givenRoleOf(String user) {
        return roles.stream().filter((r) -> r.user().equals(user)).map(UserRole::role).findFirst().orElse(Role.CLERK);
    }

    /** The steward who delegated the role to the user name by the latest delegation in force on today, else null. */
    public String delegatorOf(String user) {
        String by = null;
        for (Delegation d : delegations) {
            if (d.user().equals(user) && d.covers(today)) {
                by = d.by();
            }
        }
        return by;
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
