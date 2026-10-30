package net.officefloor.hq.app.note;

/** A note that passed validation: trimmed and not blank. */
public record ValidNote(String text, String user) {
}
