package net.officefloor.hq.app.voidreason;

/** A void reason on a sheet: while the sheet has any, a claim is voided only with one of them. */
public record VoidReason(String sheetId, String reason) {
}
