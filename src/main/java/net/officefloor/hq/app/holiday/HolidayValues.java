package net.officefloor.hq.app.holiday;

import java.time.LocalDate;

/** A holiday to add once validated: its date, and who adds it. */
public record HolidayValues(LocalDate date, String by) {
}
