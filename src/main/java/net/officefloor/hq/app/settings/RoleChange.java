package net.officefloor.hq.app.settings;

/** A validated role change on a sheet. */
public record RoleChange(String sheetId, String name, Role role, String by) {
}
