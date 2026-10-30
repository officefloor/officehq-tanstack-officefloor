package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A setting that passed validation: its key and its value at two decimals (terms: whole days)
 *  (a clerk limit of null: no limit); a policy date has its date in date (null: blank) and no value. */
public record ValidSetting(String key, BigDecimal value, String user, LocalDate date) {

    public ValidSetting(String key, BigDecimal value, String user) {
        this(key, value, user, null);
    }
}
