package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/merge: the id of the person merged into, and who merges. */
public record MergeRequest(String intoId, String user) {
}
