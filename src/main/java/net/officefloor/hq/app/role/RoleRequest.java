package net.officefloor.hq.app.role;

/** Body of a role assignment: who is making it, the user name it is for, and the role (clerk, steward, viewer or restricted). */
public record RoleRequest(String user, String name, String role) {
}
