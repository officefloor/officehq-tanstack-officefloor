package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A validated pot with two decimals; {@code value} null: blank, none. {@code given} false: left as it is. */
public record Pot(boolean given, BigDecimal value) {
}
