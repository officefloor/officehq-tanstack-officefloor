package net.officefloor.hq.app.note;

/** Body of a note add: who is adding it and the text as typed. */
public record NoteRequest(String user, String text) {
}
