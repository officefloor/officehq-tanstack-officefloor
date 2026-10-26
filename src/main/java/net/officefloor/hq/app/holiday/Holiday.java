package net.officefloor.hq.app.holiday;

import java.time.LocalDate;

/** A holiday to add to the sheet. */
public record Holiday(String sheetId, LocalDate date) {
}
