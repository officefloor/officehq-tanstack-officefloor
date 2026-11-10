package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated period request: its trimmed name, its to-date, and its from-date (null: the sheet's today). */
public record PeriodInput(String name, LocalDate from, LocalDate to) {
}
