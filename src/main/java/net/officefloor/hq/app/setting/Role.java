package net.officefloor.hq.app.setting;

/** The role a sheet gives a user name: clerk or steward. */
public record Role(String sheetId, String user, String role) {
}
