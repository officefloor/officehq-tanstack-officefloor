package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated period to add: a non-empty name, and from on or before to. */
public record PeriodInput(String name, LocalDate from, LocalDate to) {
}
