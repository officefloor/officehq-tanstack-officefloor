package net.officefloor.hq.app.note;

/** A note on the sheet: the sheet's today when it was added (yyyy-MM-dd), who added it, and its text. */
public record Note(long id, String on, String by, String text) {
}
