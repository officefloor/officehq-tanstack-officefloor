package net.officefloor.hq.app.access;

/** A user name, the role to assign it (clerk or steward), and the signed-in user (`by`) making the change. */
public record RoleRequest(String user, String role, String by) {
}
