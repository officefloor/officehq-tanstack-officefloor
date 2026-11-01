package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated generate body: a from and to date, the from on or before the to. */
public record GeneratePeriodsInput(LocalDate from, LocalDate to) {
}
