package net.officefloor.hq.app.setting;

/** The sheet's settings as the front end reads them: {@code clerkLimit} has two decimals, null for
 *  no limit; {@code floor} has two decimals; {@code terms} is the whole days after its date a claim is
 *  due by. */
public record SettingsResponse(String clerkLimit, String floor, String terms) {
}
