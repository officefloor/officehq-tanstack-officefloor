package net.officefloor.hq.app.setting;

import java.time.LocalDate;

/** A validated period numbering from date; {@code value} null: blank, off. {@code given} false: left as it is. */
public record PeriodNumberingFrom(boolean given, LocalDate value) {
}
