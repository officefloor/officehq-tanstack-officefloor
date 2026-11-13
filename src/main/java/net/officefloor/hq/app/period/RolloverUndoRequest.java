package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/periods/rollover-undo: who undoes the last rollover. */
public record RolloverUndoRequest(String user) {
}
