package net.officefloor.hq.app.role;

/** Body of giving a user a role: the role (clerk, steward or viewer), and who gives it. */
public record RoleRequest(String role, String by) {
}
