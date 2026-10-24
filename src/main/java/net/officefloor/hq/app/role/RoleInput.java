package net.officefloor.hq.app.role;

/** A validated role form: a non-blank user name and clerk or steward. */
public record RoleInput(String user, String role) {
}
