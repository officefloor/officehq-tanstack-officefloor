package net.officefloor.hq.app.note;

import java.time.LocalDate;

/** A note on the sheet: its text, the user who added it and the sheet's today when it was added. */
public record Note(String sheetId, LocalDate date, String user, String text) {
}
