package net.officefloor.hq.app.note;

/** Body of POST /api/sheets/{sheetId}/notes: the note's text and the signed-in user adding it. */
public record NoteRequest(String text, String user) {
}
