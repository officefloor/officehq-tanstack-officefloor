package net.officefloor.hq.app.role;

/** A user's role on a sheet: clerk (the default for any name), steward,
 * viewer (sees everything, changes nothing) or restricted (acts as a clerk, but the people list shows
 * only the people of the crews they lead or belong to, and themselves). */
public record Role(String sheetId, String user, String role) {
    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";
    public static final String VIEWER = "viewer";
    public static final String RESTRICTED = "restricted";
}
