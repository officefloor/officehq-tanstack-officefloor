package net.officefloor.hq.app.role;

/** A role as the screen sends it: a user name and a role (clerk, steward or viewer). */
public record RoleRequest(String user, String role) {
}
