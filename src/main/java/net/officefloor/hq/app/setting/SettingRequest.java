package net.officefloor.hq.app.setting;

/**
 * Body of POST /api/sheets/{sheet}/settings: the setting's key and its new value (blank: its default), and for a dated
 * key the date it is in force from (yyyy-mm-dd; blank: the sheet's today).
 */
public record SettingRequest(String key, String value, String from) {
}
