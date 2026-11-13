package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated proof threshold with two decimals; {@code value} null: none. {@code given} false: left as it is. {@code from} null: none typed, the sheet's today. */
public record ProofThreshold(boolean given, BigDecimal value, LocalDate from) {
}
