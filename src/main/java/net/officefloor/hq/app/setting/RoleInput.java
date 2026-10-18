package net.officefloor.hq.app.setting;

/** A validated role to assign: a non-empty user name (trimmed) and clerk or steward. */
public record RoleInput(String user, String role) {
}
