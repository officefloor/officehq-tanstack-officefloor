package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated note threshold with two decimals; {@code value} null: none. {@code given} false: left as it is. {@code from} null: none typed, the sheet's today. */
public record NoteThreshold(boolean given, BigDecimal value, LocalDate from) {
}
