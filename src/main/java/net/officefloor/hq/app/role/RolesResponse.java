package net.officefloor.hq.app.role;

import java.util.List;

/** The roles assigned on a sheet, by user name. */
public record RolesResponse(List<RoleResponse> roles) {
}
