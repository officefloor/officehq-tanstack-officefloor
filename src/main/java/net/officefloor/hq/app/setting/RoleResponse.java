package net.officefloor.hq.app.setting;

/** A user name's role as shown. */
public record RoleResponse(String user, String role) {

    public static RoleResponse from(UserRole role) {
        return new RoleResponse(role.getUser(), role.getRole());
    }
}
