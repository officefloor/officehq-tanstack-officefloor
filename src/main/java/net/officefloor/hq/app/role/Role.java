package net.officefloor.hq.app.role;

/** A user's role on a sheet: clerk (the default for any name), steward or
 * viewer (sees everything, changes nothing). */
public record Role(String sheetId, String user, String role) {
    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";
    public static final String VIEWER = "viewer";
}
