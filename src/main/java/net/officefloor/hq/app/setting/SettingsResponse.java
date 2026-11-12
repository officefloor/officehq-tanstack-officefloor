package net.officefloor.hq.app.setting;

/** The sheet's settings as the front end reads them: {@code clerkLimit} has two decimals, null for
 *  no limit. */
public record SettingsResponse(String clerkLimit) {
}
