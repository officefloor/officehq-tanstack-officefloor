package net.officefloor.hq.app.setting;

import java.util.List;

/** The roles assigned on a sheet, by user name; a name not listed is a clerk. */
public record RolesResponse(List<RoleResponse> roles) {

    public static RolesResponse from(List<UserRole> roles) {
        return new RolesResponse(roles.stream().map(RoleResponse::from).toList());
    }
}
