package net.officefloor.hq.app.setting;

/**
 * The sheet's settings as the screen shows them: the clerk limit with two decimals, or "" for no limit; and the floor
 * with two decimals; and the terms, a whole number of days; and the late percent with two decimals; and the policy date
 * (yyyy-MM-dd), or "" for the sheet's today.
 */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate) {
}
