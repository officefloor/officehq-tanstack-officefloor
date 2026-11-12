package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated period to add: its trimmed name, its from and to dates (inclusive) and who adds it. */
public record PeriodInput(String name, LocalDate from, LocalDate to, String user) {
}
