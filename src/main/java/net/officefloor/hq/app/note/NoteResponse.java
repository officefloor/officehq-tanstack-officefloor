package net.officefloor.hq.app.note;

/** A note as the screen reads it: the sheet's today when it was added (yyyy-MM-dd), its user and its text. */
public record NoteResponse(String date, String user, String text) {
}
