package net.officefloor.hq.app.setting;

/** A validated setting change: the key, its value as stored (null when blank) and who changes it. */
public record SettingInput(String key, String value, String user) {
}
