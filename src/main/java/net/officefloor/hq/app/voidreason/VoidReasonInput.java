package net.officefloor.hq.app.voidreason;

/** A void reason to add, validated and trimmed, and the user adding it. */
public record VoidReasonInput(String reason, String user) {
}
