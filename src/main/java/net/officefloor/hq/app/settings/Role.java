package net.officefloor.hq.app.settings;

/** The role a user name holds on a sheet: clerk, steward, viewer or restricted. */
public record Role(String user, String role) {
}
