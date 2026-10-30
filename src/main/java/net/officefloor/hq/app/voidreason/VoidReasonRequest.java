package net.officefloor.hq.app.voidreason;

/** Body of POST /api/sheets/{sheetId}/void-reasons: the reason to add to the sheet's list. */
public record VoidReasonRequest(String reason, String user) {
}
