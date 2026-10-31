package net.officefloor.hq.app.holiday;

import java.time.LocalDate;

/** A holiday on a sheet, added by `by`. */
public record Holiday(String sheetId, LocalDate date, String by) {
}
