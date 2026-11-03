package net.officefloor.hq.app.person;

/** The merge form: the id of the person to merge this person into. */
public record MergeRequest(Long intoId) {
}
