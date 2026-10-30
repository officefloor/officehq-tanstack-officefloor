package net.officefloor.hq.app.sheet;

/** Body of POST /api/sheets/{sheetId}/clone: the id of the sheet to clone to, and who clones it. */
public record CloneRequest(String toSheet, String user) {
}
