package net.officefloor.hq.app.holiday;

import java.time.LocalDate;

/** A date on the sheet's list of holidays: a new claim's due-by falling on one is moved to the next day that is not one. */
public record Holiday(long id, String sheetId, LocalDate date, String by) {
}
