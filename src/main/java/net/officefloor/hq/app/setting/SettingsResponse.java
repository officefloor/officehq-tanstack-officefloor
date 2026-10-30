package net.officefloor.hq.app.setting;

import java.util.List;

/** The sheet's settings: the clerk limit at two decimals ("" for no limit), the floor at two decimals
 *  the terms in days, the late percent at two decimals, the policy date ("" for blank), the period numbering from ("" for off) the roles set and each grade's own terms in days, by grade (a grade not in it uses the sheet's terms) and the cap at two decimals ("" for none). */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom, List<RoleResponse> roles, java.util.Map<String, String> gradeTerms, String cap) {
}
