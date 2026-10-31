package net.officefloor.hq.app.settings;

/** Body of a settings change: the clerk limit (two decimals, blank for no limit) and who makes the change. */
public record SettingsRequest(String clerkLimit, String user) {
}
