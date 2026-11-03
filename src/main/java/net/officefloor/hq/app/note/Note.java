package net.officefloor.hq.app.note;

/** A sheet's note: its id, the sheet's today when it was added (yyyy-MM-dd), who added it and its text. */
public record Note(long id, String addedOn, String addedBy, String text) {
}
