package net.officefloor.hq.app.note;

/** A note as the front end reads it: the sheet's today when it was added (yyyy-mm-dd), who added it, its text. */
public record NoteResponse(long id, String date, String user, String text) {
}
