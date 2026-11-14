package net.officefloor.hq.app.setting;

/** The role a user name has on a sheet: clerk (any name without one) or steward. */
public record Role(String sheetId, String user, String role) {
    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";
}
