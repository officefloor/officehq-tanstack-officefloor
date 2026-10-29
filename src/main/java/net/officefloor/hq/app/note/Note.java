package net.officefloor.hq.app.note;

/** A note to add to the sheet: its text, by the user, on the sheet's today. */
public record Note(String sheetId, String text, String by, java.time.LocalDate on) {
}
