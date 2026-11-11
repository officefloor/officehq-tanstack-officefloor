package net.officefloor.hq.app.role;

/** A validated role request: a trimmed user name and a role (clerk, steward, viewer or restricted). */
public record RoleInput(String user, String role) {
}
