package net.officefloor.hq.app.person;

/** A validated merge: the signed-in user, and the id of the person merged into. */
public record MergeInput(String user, long intoId) {
}
