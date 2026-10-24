package net.officefloor.hq.app.role;

/** The role form as submitted: a user name and the role to give it. */
public record RoleRequest(String user, String role) {
}
