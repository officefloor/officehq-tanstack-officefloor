package net.officefloor.hq.app.voidreason;

/** A void reason as the screen sees it: its text, and how many VOID claims were voided with it. */
public record VoidReasonResponse(String reason, int voided) {
}
