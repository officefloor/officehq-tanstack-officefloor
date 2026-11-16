package net.officefloor.hq.app.setting;

/** A setting once validated: its key, its value (null for blank), and who sets it. */
public record SettingValues(String key, String value, String by) {
}
