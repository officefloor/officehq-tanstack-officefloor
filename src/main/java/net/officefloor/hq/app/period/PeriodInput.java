package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated period: the trimmed name, and its from-date (null: the sheet's today) and to-date, and its cap (null: none). */
public record PeriodInput(String name, LocalDate from, LocalDate to, Integer cap) {
}
