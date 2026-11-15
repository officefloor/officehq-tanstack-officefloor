package net.officefloor.hq.app.note;

/** A note on a sheet: its text (trimmed), who added it and the sheet's today then (yyyy-MM-dd). */
public record Note(long id, String sheetId, String text, String by, String addedOn) {
}
