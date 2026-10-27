package net.officefloor.hq.app.person;

/** The body of a merge: the person on the sheet the merged person goes into. */
public record MergeRequest(Long intoId) {
}
