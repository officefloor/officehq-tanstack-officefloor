package net.officefloor.hq.app.claim;

/** The body of a status change: the reason a reject gives (ignored by the other changes). */
public record ClaimStatusRequest(String reason) {
}
