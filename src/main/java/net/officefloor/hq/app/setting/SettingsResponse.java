package net.officefloor.hq.app.setting;

import java.util.List;

/** The sheet's settings: the clerk limit at two decimals ("" for no limit), the floor at two decimals
 *  and the roles set. */
public record SettingsResponse(String clerkLimit, String floor, List<RoleResponse> roles) {
}
