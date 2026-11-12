package net.officefloor.hq.app.role;

/** A user's role as the front end reads it: clerk or steward. */
public record RoleResponse(String user, String role) {
}
