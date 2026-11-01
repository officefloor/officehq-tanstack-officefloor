package net.officefloor.hq.app.note;

import java.time.LocalDate;

/** A note on a sheet: its text, added by `by` while the sheet's today was `addedOn`. */
public record Note(String sheetId, String text, String by, LocalDate addedOn) {
}
