package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A validated alert threshold with two decimals; {@code value} null: blank, off. {@code given} false: left as it is. */
public record AlertThreshold(boolean given, BigDecimal value) {
}
