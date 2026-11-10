package net.officefloor.hq.app.role;

/** A validated role request: a trimmed user name and a role (clerk or steward). */
public record RoleInput(String user, String role) {
}
