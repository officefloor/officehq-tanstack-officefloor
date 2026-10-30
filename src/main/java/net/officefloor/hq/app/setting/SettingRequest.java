package net.officefloor.hq.app.setting;

/** Body of POST /api/sheets/{sheetId}/settings: the setting's key, its new value and, for a dated
 *  setting, the date it is in force from (blank: the sheet's today). */
public record SettingRequest(String key, String value, String user, String from) {
}
