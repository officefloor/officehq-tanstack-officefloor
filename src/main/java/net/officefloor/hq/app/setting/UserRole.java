package net.officefloor.hq.app.setting;

/** The role a user name has on a sheet. */
public record UserRole(String sheetId, String user, Role role) {
}
