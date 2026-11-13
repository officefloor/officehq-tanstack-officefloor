package net.officefloor.hq.app.setting;

import java.time.LocalDate;

/** A validated rounding policy date; {@code value} null: blank, the sheet's today. {@code given} false: left as it is. */
public record PolicyDate(boolean given, LocalDate value) {
}
