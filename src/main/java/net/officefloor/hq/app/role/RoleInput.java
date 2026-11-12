package net.officefloor.hq.app.role;

/** A validated role assignment: the (trimmed) user name, its role and who assigns it. */
public record RoleInput(String user, Role role, String by) {
}
