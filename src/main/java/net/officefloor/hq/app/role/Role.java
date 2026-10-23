package net.officefloor.hq.app.role;

/** A user's role on a sheet: the user name and clerk, steward, viewer (sees everything, changes nothing) or
 *  restricted (sees on the people list only the people of the crews they lead or belong to, and themselves). */
public record Role(String sheetId, String user, String role) {
    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";
    public static final String VIEWER = "viewer";
    public static final String RESTRICTED = "restricted";
}
