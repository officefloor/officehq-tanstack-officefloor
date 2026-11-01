package net.officefloor.hq.app.note;

/** Body of a note added to the sheet: its text and who adds it. */
public record NoteRequest(String text, String user) {
}
