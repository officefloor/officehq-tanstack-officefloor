package net.officefloor.hq.app.setting;

/** A validated setting: a known key and its value as stored ("" for the default). */
public record SettingInput(String key, String value) {
}
