package net.officefloor.hq.app.setting;

/** A sheet's setting: its key and value as text (null: blank). */
public record Setting(String sheetId, String key, String value) {
}
