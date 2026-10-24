package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated period form: the trimmed name, and its from and to date, from on or before to. */
public record PeriodInput(String name, LocalDate from, LocalDate to) {
}
