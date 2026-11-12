package net.officefloor.hq.app.role;

/** A user's role as the front end reads it: clerk, steward or viewer. */
public record RoleResponse(String user, String role) {
}
