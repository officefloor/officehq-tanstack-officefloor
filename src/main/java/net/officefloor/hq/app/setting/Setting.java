package net.officefloor.hq.app.setting;

/** A sheet setting: its key and its value as text ("" when blank). */
public record Setting(String sheetId, String key, String value) {
}
