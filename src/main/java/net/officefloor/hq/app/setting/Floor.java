package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A validated floor with two decimals; {@code value} null: left as it is. */
public record Floor(BigDecimal value) {
}
