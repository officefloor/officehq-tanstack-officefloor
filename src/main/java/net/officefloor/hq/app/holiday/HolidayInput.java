package net.officefloor.hq.app.holiday;

import java.time.LocalDate;

/** A validated holiday add: who is adding it and the date. */
public record HolidayInput(String user, LocalDate date) {
}
