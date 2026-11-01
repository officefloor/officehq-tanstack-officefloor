package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated period body: a name and a from and to date, inclusive, the from on or before the to. */
public record PeriodInput(String name, LocalDate from, LocalDate to) {
}
