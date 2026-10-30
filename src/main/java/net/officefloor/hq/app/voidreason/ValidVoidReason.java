package net.officefloor.hq.app.voidreason;

/** A void reason that passed validation: trimmed, not blank and not yet on the sheet's list. */
public record ValidVoidReason(String reason, String user) {
}
