package net.officefloor.hq.app.setting;

/** A role assignment that passed validation: a user name and clerk or steward. */
public record ValidRole(String name, String role, String user) {
}
