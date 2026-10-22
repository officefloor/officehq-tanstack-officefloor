package net.officefloor.hq.app.voidreason;

/** A reason on the sheet's list of void reasons: while the list is not empty, a claim is voided with one of them. */
public record VoidReason(long id, String sheetId, String reason, String by) {
}
