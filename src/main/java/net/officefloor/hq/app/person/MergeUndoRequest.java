package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/merge-undo: who undoes the merge. */
public record MergeUndoRequest(String user) {
}
