package net.officefloor.hq.app.settings;

/** Body of a settings change: the settings being set, each left out (null) when it is not being
 *  changed — the clerk limit (two decimals, blank for no limit), the floor (two decimals, blank for
 *  the default 1.00) — and who makes the change. */
public record SettingsRequest(String clerkLimit, String floor, String user) {
}
