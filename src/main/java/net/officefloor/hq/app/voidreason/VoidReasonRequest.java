package net.officefloor.hq.app.voidreason;

/** Body of POST /api/sheets/{sheet}/void-reasons: the reason added to the sheet's list. */
public record VoidReasonRequest(String reason) {
}
