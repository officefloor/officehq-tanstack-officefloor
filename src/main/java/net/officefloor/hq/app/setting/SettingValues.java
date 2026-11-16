package net.officefloor.hq.app.setting;

import java.time.LocalDate;

/** A setting once validated: its key, its value (null for blank), the from-date given (null: none given — the
 *  sheet's today for a dated key), whether the key is dated, and who sets it. */
public record SettingValues(String key, String value, LocalDate from, boolean dated, String by) {
}
