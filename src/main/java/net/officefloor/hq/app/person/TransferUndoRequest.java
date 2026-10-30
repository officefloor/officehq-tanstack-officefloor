package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/transfer-undo: who undoes the transfer. */
public record TransferUndoRequest(String user) {
}
