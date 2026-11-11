package net.officefloor.hq.app.note;

import java.time.LocalDate;

/** A note on a sheet: its text, who added it and the sheet's today when they did. */
public record Note(String sheetId, LocalDate addedOn, String addedBy, String text) {
}
