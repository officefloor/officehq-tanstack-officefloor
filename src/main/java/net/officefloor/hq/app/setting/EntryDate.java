package net.officefloor.hq.app.setting;

import java.time.LocalDate;

/** A validated entry date; {@code value} null: blank, off. {@code given} false: left as it is. */
public record EntryDate(boolean given, LocalDate value) {
}
