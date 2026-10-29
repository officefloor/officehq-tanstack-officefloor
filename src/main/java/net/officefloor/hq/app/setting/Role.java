package net.officefloor.hq.app.setting;

/** The role a sheet gives a user name: clerk, steward, viewer or restricted. */
public record Role(String sheetId, String user, String role) {
}
