package net.officefloor.hq.app.voidreason;

/** A validated void reason add: who is adding it and the reason, trimmed. */
public record VoidReasonInput(String user, String reason) {
}
