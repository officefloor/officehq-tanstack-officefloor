package net.officefloor.hq.app.claim;

/** A claim's status change: the reason, for a reject (ignored by the other actions). */
public record ClaimActionRequest(String reason) {
}
