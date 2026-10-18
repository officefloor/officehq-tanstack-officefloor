package net.officefloor.hq.app.setting;

/** The body of assigning a role: the user name and the role (clerk or steward). */
public record RoleRequest(String user, String role) {
}
