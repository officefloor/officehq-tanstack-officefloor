package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated generate request: its to-date, and its from-date (null: the sheet's today). */
public record GeneratePeriodsInput(LocalDate from, LocalDate to) {
}
