package net.officefloor.hq.app.setting;

/** The role a user name has on a sheet: clerk (any name without one), steward, or viewer (sees
 *  everything and may change nothing), or restricted (sees on the people list only the people of the
 *  crews they lead or belong to, and themselves). */
public record Role(String sheetId, String user, String role) {
    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";
    public static final String VIEWER = "viewer";
    public static final String RESTRICTED = "restricted";
}
