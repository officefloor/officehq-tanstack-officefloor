package net.officefloor.hq.app.access;

public record RoleResponse(String user, String role) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(role.user(), role.role());
    }
}
