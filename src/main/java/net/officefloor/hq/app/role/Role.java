package net.officefloor.hq.app.role;

/** The role a user name has on a sheet: clerk, steward or viewer. */
public record Role(String sheetId, String user, String role) {
}
