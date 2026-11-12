package net.officefloor.hq.app.voidreason;

/** Body of POST /api/sheets/{sheetId}/void-reasons: the reason and the user who adds it. */
public record VoidReasonRequest(String reason, String user) {
}
