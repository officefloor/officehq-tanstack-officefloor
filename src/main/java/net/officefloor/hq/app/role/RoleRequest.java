package net.officefloor.hq.app.role;

/** Body of giving a user a role: the role (clerk or steward), and who gives it. */
public record RoleRequest(String role, String by) {
}
