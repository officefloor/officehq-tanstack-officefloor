package net.officefloor.hq.app.setting;

/** Body of POST /api/sheets/{sheetId}/roles: the user name given the role, and who gives it. */
public record RoleRequest(String name, String role, String user) {
}
