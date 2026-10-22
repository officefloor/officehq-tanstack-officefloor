package net.officefloor.hq.app.holiday;

import java.time.LocalDate;

/** A date on the sheet's list of holidays: it is not a business day, so no claim's due-by falls on one. */
public record Holiday(long id, String sheetId, LocalDate date, String by) {
}
