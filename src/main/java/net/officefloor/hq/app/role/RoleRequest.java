package net.officefloor.hq.app.role;

/** Body of POST /api/sheets/{sheetId}/roles: the user name, its role and who assigns it. */
public record RoleRequest(String user, String role, String by) {
}
