package net.officefloor.hq.app.holiday;

import java.time.LocalDate;

/** A holiday that passed validation: a date not yet on the sheet's list. */
public record ValidHoliday(LocalDate date, String user) {
}
