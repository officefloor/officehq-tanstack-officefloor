package net.officefloor.hq.app.claim;

/** What a status change submits: the reason, for a reject (ignored otherwise; null: none). */
public record ClaimActionRequest(String reason) {
}
