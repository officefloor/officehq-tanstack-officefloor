package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A period once validated: its name without outer spaces, its dates, and who adds it. */
public record PeriodValues(String name, LocalDate from, LocalDate to, String by) {
}
