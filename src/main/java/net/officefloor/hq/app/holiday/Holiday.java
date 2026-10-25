package net.officefloor.hq.app.holiday;

import java.time.LocalDate;

/** A holiday added to a sheet. */
public record Holiday(String sheetId, LocalDate date) {
}
