package net.officefloor.hq.app.access;

/** A setting on a sheet: its key and value (null: blank), and the from-date it was saved from when one was given (null otherwise). */
public record Setting(String sheetId, String key, String value, java.time.LocalDate from) {

    /** Whether the key's setting is dated: one that carries a figure (every key but the policy date, period numbering from, the entry date, the grace days, the escalation gap, the review SLA, the fiscal start month, the pot, the alert threshold, the parent, the proof kinds, the default grade and the permissions). */
    public static boolean isDated(String key) {
        return !POLICY_DATE.equals(key) && !POT.equals(key) && !ALERT_THRESHOLD.equals(key) && !PARENT.equals(key) && !PERIOD_NUMBERING_FROM.equals(key) && !ENTRY_DATE.equals(key) && !GRACE_DAYS.equals(key) && !ESCALATE_AFTER.equals(key) && !REVIEW_SLA.equals(key) && !FISCAL_START_MONTH.equals(key) && !PROOF_KINDS.equals(key) && !DEFAULT_GRADE.equals(key) && !isPermission(key);
    }

    /** Whether the key is one of the permission matrix's (perm-<action>). */
    public static boolean isPermission(String key) {
        return key.startsWith(PERMISSION) && PERMISSION_STANDING.containsKey(key.substring(PERMISSION.length()));
    }

    /** The value a dated key has before every from-date: the floor's, the terms' and the late percent's starting value; null (blank) for the rest. */
    public static String starting(String key) {
        return switch (key) {
            case FLOOR -> FLOOR_DEFAULT;
            case TERMS -> TERMS_DEFAULT;
            case LATE_PCT -> LATE_PCT_DEFAULT;
            default -> null;
        };
    }

    /** The most a clerk may settle a claim's due for; blank means no limit. */
    public static final String CLERK_LIMIT = "clerk-limit";

    /** The due above which a claim needs a proof before it is settled or approved; blank means none. */
    public static final String PROOF_THRESHOLD = "proof-threshold";

    /** A grade's own proof threshold (the key is this plus the grade, proof-threshold-A); blank means the sheet's. */
    public static final String PROOF_THRESHOLD_OF_GRADE = "proof-threshold-";

    /** The proof kinds: a comma separated list of kinds (receipt, photo) a claim above the proof threshold needs at least one proof of each of before it is settled or approved; blank means none. Not dated. */
    public static final String PROOF_KINDS = "proof-kinds";

    /** The due above which a claim needs a note before it is opened; blank means none. */
    public static final String NOTE_THRESHOLD = "note-threshold";

    /** The due above which an opened claim goes to REVIEW (a steward's review opens it) instead of OPEN; blank means none. */
    public static final String REVIEW_THRESHOLD = "review-threshold";

    /** The due-total above which a person crossing it writes an ALERT line; an amount with two decimals, blank for off. Not dated. */
    public static final String ALERT_THRESHOLD = "alert-threshold";

    /** The most a person's due-total may be; an amount with two decimals, blank for none. */
    public static final String CAP = "cap";

    /** The sheet's parent: the id of a sheet whose rates (then its parent's, up to three levels up) stand in for a grade without a rate in force on a date; blank for none. */
    public static final String PARENT = "parent";

    /** The sheet's pot: the pot of each pool that is a member of none, cut among its members by their shares when it is balanced (a member pool's cut is its own pot); an amount with two decimals, blank for none. */
    public static final String POT = "pot";

    /** The least a claim's live due may be; an amount with two decimals. */
    public static final String FLOOR = "floor";

    /** The floor a sheet starts with, until it is set. */
    public static final String FLOOR_DEFAULT = "1.00";

    /** The number of days after its date a claim is due by; a whole number. */
    public static final String TERMS = "terms";

    /** The terms a sheet starts with, until they are set. */
    public static final String TERMS_DEFAULT = "14";

    /** A grade's own terms (the key is this plus the grade, terms-A); blank means the sheet's terms. */
    public static final String TERMS_OF_GRADE = "terms-";

    /** A grade's grace (the key is this plus the grade, grace-A): the days past its due-by a claim of that grade on its date goes overdue and counts its late charge from; blank means none. */
    public static final String GRACE_OF_GRADE = "grace-";

    /** The grades that may have their own terms, grace and proof threshold. */
    public static final java.util.Set<String> GRADES = java.util.Set.of("A", "B", "C");

    /** The percent of its figure an overdue claim's due gains for every full 30 days it is overdue, the one in force on each 30-day boundary date; two decimals. */
    public static final String LATE_PCT = "late-pct";

    /** The late percent a sheet starts with, until it is set. */
    public static final String LATE_PCT_DEFAULT = "0.00";

    /** The date before which a claim's live figures round half up, and from which half to even; blank means the sheet's today. */
    public static final String POLICY_DATE = "policy-date";

    /** How a claim's live figures round on its date (half-up or half-even); blank means none, the policy date decides. */
    public static final String ROUNDING = "rounding";

    /** The rounding mode that rounds half up. */
    public static final String ROUNDING_HALF_UP = "half-up";

    /** The rounding mode that rounds half to even. */
    public static final String ROUNDING_HALF_EVEN = "half-even";

    /** The date from which (on the sheet's today) an opened claim is numbered within its period; blank means off. */
    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";

    /** The default date of a new claim (the claim form, the quick-add bar, a pasted line, a correction) in place of the sheet's today; blank means off. */
    public static final String ENTRY_DATE = "entry-date";

    /** The days after its to-date an open period is closed by itself when the sheet's today moves past them; a whole number, blank for off. */
    public static final String GRACE_DAYS = "grace-days";

    /** The escalation gap: the days at least which a claim's previous reminder was before the sheet's today for its next reminder to escalate it (once ever); a whole number, blank for off. Not dated. */
    public static final String ESCALATE_AFTER = "escalate-after";

    /** The review SLA: the days after its review day (the sheet's today when it went to REVIEW) a REVIEW claim may wait before the sheet's today makes it late; a whole number, blank for none. Not dated. */
    public static final String REVIEW_SLA = "review-sla";

    /** The grade a person added by the quick-add bar or an import line with a blank grade takes: one of the grades, blank for A. Not dated. */
    public static final String DEFAULT_GRADE = "default-grade";

    /** The default grade a sheet starts with, until it is set. */
    public static final String DEFAULT_GRADE_DEFAULT = "A";

    /** The month (1 to 12) the sheet's fiscal year starts in; 1 until it is set. */
    public static final String FISCAL_START_MONTH = "fiscal-start-month";

    /** The fiscal start month a sheet starts with, until it is set. */
    public static final int FISCAL_START_MONTH_DEFAULT = 1;

    /** The roles that may take an action (the key is this plus the action, perm-open): a comma separated list of roles, blank for the standing rule. */
    public static final String PERMISSION = "perm-";

    /** Each action's standing rule, the roles that may take it while its permission is blank. */
    public static final java.util.Map<String, java.util.List<String>> PERMISSION_STANDING = java.util.Map.of(
            "open", java.util.List.of(Role.CLERK, Role.STEWARD),
            "settle", java.util.List.of(Role.CLERK, Role.STEWARD),
            "void", java.util.List.of(Role.CLERK, Role.STEWARD),
            "close", java.util.List.of(Role.CLERK, Role.STEWARD),
            "reassign", java.util.List.of(Role.STEWARD),
            "reopen", java.util.List.of(Role.STEWARD));
}
