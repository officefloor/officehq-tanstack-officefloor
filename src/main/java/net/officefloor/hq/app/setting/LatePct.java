package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated late percent with two decimals; {@code value} null: left as it is. {@code from} null: none typed, the sheet's today. */
public record LatePct(BigDecimal value, LocalDate from) {
}
