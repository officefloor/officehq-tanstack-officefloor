package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A checked period's dates, the from-date resolved. */
public record PeriodDates(LocalDate from, LocalDate to) {
}
