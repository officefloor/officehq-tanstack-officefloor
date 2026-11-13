package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated floor with two decimals; {@code value} null: left as it is. {@code from} null: none typed, the sheet's today. */
public record Floor(BigDecimal value, LocalDate from) {
}
