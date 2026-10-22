package net.officefloor.hq.app.role;

/** A validated role assignment: who is making it, the user name it is for and the role. */
public record RoleInput(String user, String name, String role) {
}
