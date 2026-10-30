package net.officefloor.hq.app.setting;

/** Body of POST /api/sheets/{sheetId}/settings: the setting's key and its new value. */
public record SettingRequest(String key, String value, String user) {
}
