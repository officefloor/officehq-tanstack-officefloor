package net.officefloor.hq.app.holiday;

import java.time.LocalDate;

/** A holiday to add, validated, and the user adding it. */
public record HolidayInput(LocalDate date, String user) {
}
