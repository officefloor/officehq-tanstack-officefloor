package net.officefloor.hq.app.setting;

/** The body of saving the sheet's settings: the clerk limit (0.00 or more, up to two decimals; empty: no limit)
 *  and the floor (0.00 or more, up to two decimals; empty or absent: unchanged). */
public record SettingsRequest(String clerkLimit, String floor) {
}
