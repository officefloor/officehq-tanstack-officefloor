package net.officefloor.hq.app.role;

/** A role as the screen sends it: a user name and a role (clerk, steward, viewer or restricted). */
public record RoleRequest(String user, String role) {
}
