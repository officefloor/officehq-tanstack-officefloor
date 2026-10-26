package net.officefloor.hq.app.setting;

/** The role a sheet gives a user name: clerk, steward or viewer. */
public record Role(String sheetId, String user, String role) {
}
