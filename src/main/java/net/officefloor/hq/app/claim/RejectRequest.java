package net.officefloor.hq.app.claim;

/** The body of rejecting a PENDING claim: the reason (required). */
public record RejectRequest(String reason) {
}
