package net.officefloor.hq.app.access;

/** A setting on a sheet: its key and value (null: blank). */
public record Setting(String sheetId, String key, String value) {

    /** The most a clerk may settle a claim's due for; blank means no limit. */
    public static final String CLERK_LIMIT = "clerk-limit";
}
