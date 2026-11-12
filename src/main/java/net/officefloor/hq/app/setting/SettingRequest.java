package net.officefloor.hq.app.setting;

/** Body of POST /api/sheets/{sheetId}/settings: the setting's key, its new value (blank to clear it)
 *  and who changes it. */
public record SettingRequest(String key, String value, String user) {
}
