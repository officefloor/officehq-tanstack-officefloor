package net.officefloor.hq.app.sheet;

/** A clone as the screen sends it: the id of the sheet to clone this one to. */
public record CloneRequest(String toSheet) {
}
