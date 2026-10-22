package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated period add: who is making it, the trimmed name, and the from and to dates (from on or before to). */
public record PeriodInput(String user, String name, LocalDate from, LocalDate to) {
}
