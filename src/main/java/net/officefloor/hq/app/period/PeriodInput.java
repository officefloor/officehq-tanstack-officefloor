package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated period request: its trimmed name, its to-date, its from-date (null: the sheet's today) and its cap (null: none). */
public record PeriodInput(String name, LocalDate from, LocalDate to, Integer cap) {
}
