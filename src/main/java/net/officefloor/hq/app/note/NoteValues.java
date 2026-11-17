package net.officefloor.hq.app.note;

/** A note to add once validated: its text (trimmed, not blank), and who adds it. */
public record NoteValues(String text, String by) {
}
