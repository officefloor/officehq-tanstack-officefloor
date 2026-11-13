package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A validated clerk limit with two decimals; {@code value} null: no limit. {@code given} false: left as it is. */
public record ClerkLimit(boolean given, BigDecimal value) {
}
