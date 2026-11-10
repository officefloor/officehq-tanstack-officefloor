package net.officefloor.hq.app.holiday;

import java.time.LocalDate;

/** A holiday request, validated: its date. */
public record HolidayInput(LocalDate date) {
}
