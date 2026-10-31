package net.officefloor.hq.app.setting;

/** A role assignment that passed validation: a user name and clerk, steward, viewer or restricted. */
public record ValidRole(String name, String role, String user) {
}
