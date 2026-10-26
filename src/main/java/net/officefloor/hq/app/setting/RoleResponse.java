package net.officefloor.hq.app.setting;

/** A role as the screen sees it. */
public record RoleResponse(String user, String role) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(role.user(), role.role());
    }
}
