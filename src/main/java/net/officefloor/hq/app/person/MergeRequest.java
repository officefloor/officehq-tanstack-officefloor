package net.officefloor.hq.app.person;

/** Body of merging a person into another: the id of the person they merge into, and who merges them. */
public record MergeRequest(Long intoId, String by) {
}
