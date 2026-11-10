package net.officefloor.hq.app.holiday;

import java.time.LocalDate;

/** A holiday on a sheet: a claim's due-by fixed at its creation is never on one. */
public record Holiday(String sheetId, LocalDate date) {
}
