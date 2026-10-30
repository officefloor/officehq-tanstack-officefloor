package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A setting that passed validation: the clerk limit at two decimals (null for no limit). */
public record ValidSetting(String key, BigDecimal value, String user) {
}
