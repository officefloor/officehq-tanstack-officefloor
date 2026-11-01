package net.officefloor.hq.app.setting;

/** Body of POST /api/sheets/{sheet}/settings: the setting's key and its new value (blank: its default). */
public record SettingRequest(String key, String value) {
}
