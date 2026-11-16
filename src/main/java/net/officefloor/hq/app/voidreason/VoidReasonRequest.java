package net.officefloor.hq.app.voidreason;

/** Body of adding a void reason: its text, and who adds it. */
public record VoidReasonRequest(String reason, String by) {
}
