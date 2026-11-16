package net.officefloor.hq.app.setting;

/** Body of setting a sheet setting: the value as text (blank for none), and who sets it. */
public record SettingRequest(String value, String by) {
}
