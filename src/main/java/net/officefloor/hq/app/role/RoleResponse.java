package net.officefloor.hq.app.role;

/** A role as the screen shows it: the user name and the role. */
public record RoleResponse(String user, String role) {
    public static RoleResponse from(Role r) {
        return new RoleResponse(r.user(), r.role());
    }
}
