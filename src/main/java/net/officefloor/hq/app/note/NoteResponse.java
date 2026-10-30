package net.officefloor.hq.app.note;

/** A sheet note as shown: the sheet's today when it was added (YYYY-MM-DD), who added it, and its text. */
public record NoteResponse(String on, String user, String text) {
}
