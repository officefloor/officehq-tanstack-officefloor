package net.officefloor.hq.app.person;

/** Body of a merge: the id of the person on the sheet the person is merged into. */
public record MergeRequest(Long intoId) {
}
