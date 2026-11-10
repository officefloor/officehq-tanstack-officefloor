package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

/**
 * A sheet's settings. The figure settings are dated ({@code dated}, by key, each value from a
 * from-date; a blank value is "none"): its clerk limit, the highest due a clerk may settle (none: no
 * limit), its floor, the least a claim's live due may be, and its terms, the days after a claim's
 * date that it is due by, and its late percent, what an overdue claim's due gains per full 30 days
 * overdue, and its terms per grade (a grade with none uses the sheet's terms), and its cap (none:
 * no cap), the most a person's due-total may be, and its proof threshold (none: none), above which a
 * claim's due, on the claim's date, needs a proof before it is settled or approved. The value in force on a date is the one with the
 * latest from-date on or before it; before every from-date the starting value (floor 1.00, late
 * percent 0.00, terms 14; none for the others). A claim takes its floor, late percent and terms on
 * its date; the clerk limit and the cap are those in force on the sheet's today ({@code today}).
 * Not dated: its policy date (null: the sheet's today): a claim dated before it rounds half up, one
 * on or after it half to even, and its period numbering from (null: off): while the sheet's today
 * is on or after it, a claim opened is numbered within its period, and its permission matrix
 * ({@code perms}, by action, the roles that may take it as a comma separated list; blank or missing:
 * the standing rule, open, settle, void and close by clerk and steward, reassign and reopen by steward).
 */
public record Settings(String sheetId, LocalDate today, Map<String, NavigableMap<LocalDate, String>> dated,
        LocalDate policyDate, LocalDate periodNumberingFrom, Map<String, String> perms) {
    /** The grades that may have their own terms. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    public static final String CLERK_LIMIT = "clerk-limit";
    public static final String FLOOR = "floor";
    public static final String TERMS = "terms";
    public static final String LATE_PCT = "late-pct";
    public static final String CAP = "cap";
    public static final String PROOF_THRESHOLD = "proof-threshold";

    /** The actions of the permission matrix, in the order they are shown and audited. */
    public static final List<String> ACTIONS = List.of("open", "settle", "void", "reassign", "close", "reopen");

    /** The key of an action's entry in the permission matrix. */
    public static String permKey(String action) {
        return "perm-" + action;
    }

    /** The roles that may take an action by the standing rule. */
    public static List<String> standingRoles(String action) {
        return "reassign".equals(action) || "reopen".equals(action) ? List.of("steward") : List.of("clerk", "steward");
    }

    /** The roles that may take an action: its entry in the permission matrix, else the standing rule. */
    public List<String> rolesFor(String action) {
        String entry = perms == null ? null : perms.get(action);
        if (entry == null || entry.isBlank()) return standingRoles(action);
        return java.util.Arrays.stream(entry.split(",")).map(String::trim).filter((r) -> !r.isEmpty()).toList();
    }

    /** Whether a role may take an action. */
    public boolean allows(String action, String role) {
        if (net.officefloor.hq.app.role.Role.VIEWER.equals(role)) return false; // a viewer changes nothing
        return rolesFor(action).contains(role);
    }

    /** The key of a grade's own terms. */
    public static String gradeTermsKey(String grade) {
        return "terms-" + grade;
    }

    /** A sheet's floor until it is set. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** A sheet's terms until they are set. */
    public static final int DEFAULT_TERMS = 14;

    /** A sheet's late percent until it is set. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    /** The value of a key in force on a date: null when none is recorded on or before it, '' for none. */
    public String valueOn(String key, LocalDate date) {
        NavigableMap<LocalDate, String> values = dated.get(key);
        if (values == null || date == null) return null;
        Map.Entry<LocalDate, String> entry = values.floorEntry(date);
        return entry == null ? null : entry.getValue();
    }

    private BigDecimal figureOn(String key, LocalDate date) {
        String value = valueOn(key, date);
        return value == null || value.isEmpty() ? null : new BigDecimal(value).setScale(2);
    }

    /** The floor in force on a date. */
    public BigDecimal floorOn(LocalDate date) {
        BigDecimal floor = figureOn(FLOOR, date);
        return floor != null ? floor : DEFAULT_FLOOR;
    }

    /** The late percent in force on a date. */
    public BigDecimal latePctOn(LocalDate date) {
        BigDecimal pct = figureOn(LATE_PCT, date);
        return pct != null ? pct : DEFAULT_LATE_PCT;
    }

    /** The sheet's terms in force on a date. */
    public int termsOn(LocalDate date) {
        String value = valueOn(TERMS, date);
        return value == null || value.isEmpty() ? DEFAULT_TERMS : Integer.parseInt(value);
    }

    /** A grade's own terms in force on a date, or null when it takes the sheet's. */
    public Integer gradeTermsOn(String grade, LocalDate date) {
        String value = valueOn(gradeTermsKey(grade), date);
        return value == null || value.isEmpty() ? null : Integer.valueOf(value);
    }

    /** The terms of a grade on a date: its own, else the sheet's. */
    public int termsFor(String grade, LocalDate date) {
        Integer own = gradeTermsOn(grade, date);
        return own != null ? own : termsOn(date);
    }

    /** The clerk limit in force on a date (null: no limit). */
    public BigDecimal clerkLimitOn(LocalDate date) {
        return figureOn(CLERK_LIMIT, date);
    }

    /** The proof threshold in force on a date (null: none). */
    public BigDecimal proofThresholdOn(LocalDate date) {
        return figureOn(PROOF_THRESHOLD, date);
    }

    /**
     * Whether a claim dated {@code date} with a due (null: n/a) and a proofs count needs a proof
     * before it is settled or approved: its due is above the proof threshold in force on its date
     * and it has no proofs. A due of n/a, or no threshold, needs none.
     */
    public boolean needsProof(BigDecimal due, LocalDate date, int proofs) {
        BigDecimal threshold = proofThresholdOn(date);
        return proofs == 0 && threshold != null && due != null && due.compareTo(threshold) > 0;
    }

    /** The cap in force on a date (null: none). */
    public BigDecimal capOn(LocalDate date) {
        return figureOn(CAP, date);
    }

    /**
     * How every figure of a claim that rounds by {@code date} is rounded: half up before the policy
     * date ({@code today} when it is blank), half to even on or after it.
     */
    public RoundingMode rounding(LocalDate date, LocalDate today) {
        LocalDate policy = policyDate != null ? policyDate : today;
        return date.isBefore(policy) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    /** Whether a claim opened on {@code today} is numbered within its period. */
    public boolean numbersByPeriod(LocalDate today) {
        return periodNumberingFrom != null && !today.isBefore(periodNumberingFrom);
    }

    /** A person's due-total (null: n/a) held to the cap in force on {@code today}. A due-total of n/a is not capped. */
    public BigDecimal capped(BigDecimal dueTotal, LocalDate today) {
        BigDecimal cap = capOn(today);
        return cap == null || dueTotal == null || dueTotal.compareTo(cap) <= 0 ? dueTotal : cap;
    }

    /** Whether a due (null: n/a) is within the clerk limit in force on {@code today}. A due of n/a is within any limit. */
    public boolean withinClerkLimit(BigDecimal due, LocalDate today) {
        BigDecimal clerkLimit = clerkLimitOn(today);
        return clerkLimit == null || due == null || due.compareTo(clerkLimit) <= 0;
    }
}
