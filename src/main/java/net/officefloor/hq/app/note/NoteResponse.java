package net.officefloor.hq.app.note;

/** A note as the screen sees it: the sheet's today when added (yyyy-mm-dd), who added it, and its text. */
public record NoteResponse(long id, String on, String by, String text) {
}
