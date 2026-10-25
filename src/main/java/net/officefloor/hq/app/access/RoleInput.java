package net.officefloor.hq.app.access;

/** A validated role assignment: the trimmed user name and its role. */
public record RoleInput(String user, String role) {
}
