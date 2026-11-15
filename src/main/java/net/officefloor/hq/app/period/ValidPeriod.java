package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A period request that passed validation: a trimmed name and from and to dates, from on or before to, and
 *  the cap (null: none), a whole number above zero. */
public record ValidPeriod(String name, LocalDate from, LocalDate to, Integer cap) {
}
