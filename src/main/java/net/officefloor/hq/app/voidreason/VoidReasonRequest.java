package net.officefloor.hq.app.voidreason;

/** Body of a void reason add: who is adding it and the reason as typed. */
public record VoidReasonRequest(String user, String reason) {
}
