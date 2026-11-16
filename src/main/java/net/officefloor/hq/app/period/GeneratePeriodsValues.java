package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** The validated dates of generating periods, and who generates them. */
public record GeneratePeriodsValues(LocalDate from, LocalDate to, String by) {
}
