package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheet}/people/{person}/merge: the id of the person they are merged into. */
public record MergeRequest(Long into) {
}
