package net.officefloor.hq.app.setting;

/** A sheet setting: its key and its value as text ("" when blank), in force from its from-date
 *  (yyyy-MM-dd; null: from the beginning of time, and always for an undated key). */
public record Setting(String sheetId, String key, String value, String from) {
}
