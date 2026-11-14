package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A period request that passed validation: a trimmed name and from and to dates, from on or before to. */
public record ValidPeriod(String name, LocalDate from, LocalDate to) {
}
