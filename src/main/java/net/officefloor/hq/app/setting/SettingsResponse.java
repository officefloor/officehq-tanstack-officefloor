package net.officefloor.hq.app.setting;

/** The sheet's settings as the screen sees them: the clerk limit with two decimals, "" when there is none, the floor with two decimals, the terms in days, and the late percent with two decimals. */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct) {
}
