package net.officefloor.hq.app.role;

/** Body of a role assignment: who is making it, the user name it is for, and the role (clerk or steward). */
public record RoleRequest(String user, String name, String role) {
}
