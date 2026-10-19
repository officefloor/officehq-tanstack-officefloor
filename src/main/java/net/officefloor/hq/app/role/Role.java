package net.officefloor.hq.app.role;

/** The role a sheet assigns a user name: clerk (the default for any name) or steward. */
public record Role(String sheetId, String user, String role) {

    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";
}
