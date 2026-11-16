package net.officefloor.hq.app.voidreason;

/** A void reason of the sheet, and how many of its VOID claims were voided with it. */
public record VoidReason(String reason, long count) {
}
