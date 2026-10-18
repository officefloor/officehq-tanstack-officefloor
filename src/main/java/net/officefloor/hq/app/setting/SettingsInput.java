package net.officefloor.hq.app.setting;

/** Validated settings to save: the clerk limit with two decimals, or null for no limit. */
public record SettingsInput(String clerkLimit) {
}
