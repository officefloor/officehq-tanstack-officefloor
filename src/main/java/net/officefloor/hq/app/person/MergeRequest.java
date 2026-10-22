package net.officefloor.hq.app.person;

/** Body of merging a person into another: who is doing it, and the id of the person merged into. */
public record MergeRequest(String user, Long intoId) {
}
