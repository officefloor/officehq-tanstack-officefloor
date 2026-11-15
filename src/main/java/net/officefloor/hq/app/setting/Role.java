package net.officefloor.hq.app.setting;

/** The role a user name has on a sheet: clerk (any name without one), steward, or viewer (sees
 *  everything and may change nothing). */
public record Role(String sheetId, String user, String role) {
    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";
    public static final String VIEWER = "viewer";
}
