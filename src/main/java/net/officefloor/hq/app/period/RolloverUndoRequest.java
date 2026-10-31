package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/rollover-undo: the user undoing the sheet's last rollover. */
public record RolloverUndoRequest(String user) {
}
