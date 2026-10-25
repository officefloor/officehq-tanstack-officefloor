package net.officefloor.hq.app.access;

/** A role assigned to a user name on a sheet: clerk (any name without one), steward or viewer (sees everything, changes nothing). */
public record Role(String sheetId, String user, String role) {

    public static final String CLERK = "clerk";

    public static final String STEWARD = "steward";

    public static final String VIEWER = "viewer";
}
