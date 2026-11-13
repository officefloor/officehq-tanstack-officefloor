package net.officefloor.hq.app.setting;

import java.time.LocalDate;

/** Validated terms, in whole days; {@code value} null: left as it is. {@code from} null: none typed, the sheet's today. */
public record Terms(Integer value, LocalDate from) {
}
