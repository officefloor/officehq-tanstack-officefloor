package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A setting that passed validation: its key and its value at two decimals (terms: whole days)
 *  (a clerk limit or cap of null: none); a policy date or period numbering from has its date in date (null: blank) and no value.
 *  A dated setting's from is the date its value is in force from (null: none given, the sheet's today).
 *  A permission (perm-<action>) has its roles, comma separated ("" for the standing rule), and no value.
 *  The parent has the parent sheet id in text (null: none) and no value. */
public record ValidSetting(String key, BigDecimal value, String user, LocalDate date, LocalDate from, String roles, String text) {

    public ValidSetting(String key, BigDecimal value, String user, LocalDate date, LocalDate from, String roles) {
        this(key, value, user, date, from, roles, null);
    }

    public ValidSetting(String key, BigDecimal value, String user, LocalDate date, LocalDate from) {
        this(key, value, user, date, from, null, null);
    }
}
