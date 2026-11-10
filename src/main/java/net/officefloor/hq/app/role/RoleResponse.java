package net.officefloor.hq.app.role;

/** A role, as the screen reads it. */
public record RoleResponse(String user, String role) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(role.user(), role.role());
    }
}
