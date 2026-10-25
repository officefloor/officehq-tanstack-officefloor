package net.officefloor.hq.app.voidreason;

/** A void reason's text and the signed-in user (`by`) adding it. */
public record VoidReasonRequest(String reason, String by) {
}
