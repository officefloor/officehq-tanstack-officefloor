package net.officefloor.hq.app.note;

import java.time.LocalDate;

/** A note on the sheet: its text, who added it and the sheet's today when it was added. */
public record Note(long id, String sheetId, String text, String by, LocalDate on) {
}
