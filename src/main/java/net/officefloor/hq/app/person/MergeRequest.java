package net.officefloor.hq.app.person;

/** Body of a merge: the id of the person merged into, and who merges them. */
public record MergeRequest(Long into, String user) {
}
