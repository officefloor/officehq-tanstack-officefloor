package net.officefloor.hq.app.access;

/** A setting on a sheet: its key and value (null: blank). */
public record Setting(String sheetId, String key, String value) {

    /** The most a clerk may settle a claim's due for; blank means no limit. */
    public static final String CLERK_LIMIT = "clerk-limit";

    /** The most a person's due-total may be; an amount with two decimals, blank for none. */
    public static final String CAP = "cap";

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

    /** The grades that may have their own terms. */
    public static final java.util.Set<String> GRADES = java.util.Set.of("A", "B", "C");

    /** The percent of its figure an overdue claim's due gains for every full 30 days it is overdue; two decimals. */
    public static final String LATE_PCT = "late-pct";

    /** The late percent a sheet starts with, until it is set. */
    public static final String LATE_PCT_DEFAULT = "0.00";

    /** The date before which a claim's live figures round half up, and from which half to even; blank means the sheet's today. */
    public static final String POLICY_DATE = "policy-date";

    /** The date from which (on the sheet's today) an opened claim is numbered within its period; blank means off. */
    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";
}
