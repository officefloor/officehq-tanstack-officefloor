package net.officefloor.hq.app.note;

/** Body of adding a note: its text, and who adds it. */
public record NoteRequest(String text, String by) {
}
