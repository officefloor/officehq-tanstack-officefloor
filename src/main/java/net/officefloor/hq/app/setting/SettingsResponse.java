package net.officefloor.hq.app.setting;

import java.util.List;

/** The sheet's settings: the clerk limit at two decimals ("" for no limit), the floor at two decimals
 *  the terms in days, the late percent at two decimals and the roles set. */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, List<RoleResponse> roles) {
}
