package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated period form: the trimmed name, its from and to date, from on or before to, and its cap (null for none). */
public record PeriodInput(String name, LocalDate from, LocalDate to, Integer cap) {
}
