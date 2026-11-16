package net.officefloor.hq.app.role;

/** A role once validated: the user name, the role, and who gives it. */
public record RoleValues(String user, String role, String by) {
}
