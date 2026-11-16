package net.officefloor.hq.app.setting;

import java.time.LocalDate;

/** A sheet's setting: its key, its value as text (null: blank) and the date it is in force from (null: undated —
 *  a policy date or period numbering from-date, or a value from before settings were dated). */
public record Setting(String sheetId, String key, String value, LocalDate from) {
}
