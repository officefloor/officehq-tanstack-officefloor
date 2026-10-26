package net.officefloor.hq.app.person;

/** A validated merge: the id of the person the merged one goes into. */
public record MergeInput(long intoId) {
}
