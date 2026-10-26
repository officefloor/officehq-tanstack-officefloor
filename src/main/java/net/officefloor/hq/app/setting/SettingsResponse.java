package net.officefloor.hq.app.setting;

/** The sheet's settings as the screen sees them: the clerk limit with two decimals, "" when there is none, and the floor with two decimals. */
public record SettingsResponse(String clerkLimit, String floor) {
}
