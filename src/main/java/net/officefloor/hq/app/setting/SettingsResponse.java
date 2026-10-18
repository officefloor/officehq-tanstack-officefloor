package net.officefloor.hq.app.setting;

/** The sheet's settings as shown: the clerk limit with two decimals, or null for no limit; the floor with two decimals;
 *  the terms in days; the late percent with two decimals. */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct) {
}
