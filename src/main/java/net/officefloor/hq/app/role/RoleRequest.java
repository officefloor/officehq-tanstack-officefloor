package net.officefloor.hq.app.role;

/** Body of POST /api/sheets/{sheet}/roles: the user name and the role it is given. */
public record RoleRequest(String user, String role) {
}
