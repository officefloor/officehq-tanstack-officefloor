package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A validated clerk limit with two decimals; {@code value} null: no limit. */
public record ClerkLimit(BigDecimal value) {
}
