package net.officefloor.hq.app.person;

/** Body of merging a person into another: the other person, and who makes the change. */
public record MergeRequest(Long intoId, String user) {
}
