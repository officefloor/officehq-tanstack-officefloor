package net.officefloor.hq.app.role;

/** A user's role as the front end reads it: clerk, steward, viewer or restricted. */
public record RoleResponse(String user, String role) {
}
