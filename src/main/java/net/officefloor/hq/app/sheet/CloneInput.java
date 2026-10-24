package net.officefloor.hq.app.sheet;

/** A validated clone request: the id of the sheet to clone this one to, trimmed. */
public record CloneInput(String toSheet) {
}
