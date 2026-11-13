package net.officefloor.hq.app.holiday;

import java.time.LocalDate;

/** A holiday on the sheet. */
public record Holiday(String sheetId, LocalDate date) {
}
