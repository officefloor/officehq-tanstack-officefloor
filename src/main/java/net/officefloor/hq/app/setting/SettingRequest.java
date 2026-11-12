package net.officefloor.hq.app.setting;

/** Body of POST /api/sheets/{sheetId}/settings: the setting's key, its new value (blank to clear it),
 *  the date it holds from (blank for the sheet's today; a setting that is not dated ignores it) and who
 *  changes it. */
public record SettingRequest(String key, String value, String from, String user) {
}
