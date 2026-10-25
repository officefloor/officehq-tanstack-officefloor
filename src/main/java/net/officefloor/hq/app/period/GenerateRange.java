package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** The validated dates to generate monthly periods between: the from-date (null: the sheet's today) and to-date. */
public record GenerateRange(LocalDate from, LocalDate to) {
}
