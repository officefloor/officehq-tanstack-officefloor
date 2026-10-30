package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A setting that passed validation: its key and its value at two decimals
 *  (a clerk limit of null: no limit). */
public record ValidSetting(String key, BigDecimal value, String user) {
}
