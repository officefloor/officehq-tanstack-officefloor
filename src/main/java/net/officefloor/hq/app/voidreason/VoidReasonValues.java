package net.officefloor.hq.app.voidreason;

/** A void reason to add once validated: its trimmed text, and who adds it. */
public record VoidReasonValues(String reason, String by) {
}
