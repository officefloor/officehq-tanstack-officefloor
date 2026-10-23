package net.officefloor.hq.app.note;

/** A validated note add: who is adding it and the text, trimmed. */
public record NoteInput(String user, String text) {
}
