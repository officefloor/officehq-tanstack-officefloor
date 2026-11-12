package net.officefloor.hq.app.note;

/** A note to add, validated (its text trimmed), and the user adding it. */
public record NoteInput(String text, String user) {
}
