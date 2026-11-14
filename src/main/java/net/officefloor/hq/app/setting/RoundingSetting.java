package net.officefloor.hq.app.setting;

import java.time.LocalDate;

/** A validated rounding mode, half-up or half-even; {@code value} null: none. {@code given} false: left as it is. {@code from} null: none typed, the sheet's today. */
public record RoundingSetting(boolean given, String value, LocalDate from) {
}
