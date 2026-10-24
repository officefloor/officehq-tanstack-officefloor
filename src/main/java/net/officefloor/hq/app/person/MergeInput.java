package net.officefloor.hq.app.person;

/** A validated merge request: the id of the person to merge into. */
public record MergeInput(long intoId) {
}
