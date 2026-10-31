package net.officefloor.hq.app.settings;

/** Body of a role change: the user name given the role, the role (clerk or steward) and who makes the change. */
public record RoleRequest(String name, String role, String user) {
}
