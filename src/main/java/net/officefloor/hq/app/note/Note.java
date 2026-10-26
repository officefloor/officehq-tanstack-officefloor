package net.officefloor.hq.app.note;

import java.time.LocalDate;

/** A note on a sheet: the sheet's today when it was added, who added it and its text. */
public record Note(String sheetId, LocalDate addedOn, String addedBy, String text) {
}
