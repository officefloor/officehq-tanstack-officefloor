package net.officefloor.hq.app.role;

/** A validated role form: a non-blank user name and clerk, steward or viewer. */
public record RoleInput(String user, String role) {
}
