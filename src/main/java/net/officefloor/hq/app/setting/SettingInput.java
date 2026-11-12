package net.officefloor.hq.app.setting;

import java.time.LocalDate;

/** A validated setting change: the key, its value as stored (null when blank), the from-date given for a
 *  dated setting (null when blank: then the sheet's today) and who changes it. */
public record SettingInput(String key, String value, LocalDate from, String user) {
}
