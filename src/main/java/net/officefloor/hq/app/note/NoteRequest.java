package net.officefloor.hq.app.note;

/** A note's text and the signed-in user (`by`) adding it. */
public record NoteRequest(String text, String by) {
}
