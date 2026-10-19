package net.officefloor.hq.app.role;

/** A user name's role as the screen shows it. */
public record RoleResponse(String user, String role) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(role.user(), role.role());
    }
}
