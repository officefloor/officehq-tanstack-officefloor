package net.officefloor.hq.app.person;

/** A merge as the screen sends it: the id of the person to merge into. */
public record MergeRequest(String intoId) {
}
