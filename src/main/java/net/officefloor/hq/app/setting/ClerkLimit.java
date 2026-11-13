package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated clerk limit with two decimals; {@code value} null: no limit. {@code given} false: left as it is. {@code from} null: none typed, the sheet's today. */
public record ClerkLimit(boolean given, BigDecimal value, LocalDate from) {
}
