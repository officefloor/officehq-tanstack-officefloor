package net.officefloor.hq.app.voidreason;

/** A void reason on the sheet, with how many VOID claims were voided with it. */
public record VoidReason(String reason, int voided) {
}
