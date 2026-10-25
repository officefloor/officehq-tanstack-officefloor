package net.officefloor.hq.app.access;

/** A setting on a sheet: its key and value (null: blank). */
public record Setting(String sheetId, String key, String value) {

    /** The most a clerk may settle a claim's due for; blank means no limit. */
    public static final String CLERK_LIMIT = "clerk-limit";

    /** The least a claim's live due may be; an amount with two decimals. */
    public static final String FLOOR = "floor";

    /** The floor a sheet starts with, until it is set. */
    public static final String FLOOR_DEFAULT = "1.00";

    /** The number of days after its date a claim is due by; a whole number. */
    public static final String TERMS = "terms";

    /** The terms a sheet starts with, until they are set. */
    public static final String TERMS_DEFAULT = "14";
}
