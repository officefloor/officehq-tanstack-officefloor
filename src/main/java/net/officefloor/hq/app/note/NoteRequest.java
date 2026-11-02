package net.officefloor.hq.app.note;

/** Body of POST /api/sheets/{sheet}/notes: the text of the note added to the sheet. */
public record NoteRequest(String text) {
}
