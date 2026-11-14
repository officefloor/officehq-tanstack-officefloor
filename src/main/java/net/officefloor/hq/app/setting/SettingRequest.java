package net.officefloor.hq.app.setting;

/** Body of changing a setting; a blank value clears it. */
public record SettingRequest(String key, String value) {
}
