package net.officefloor.hq.app.claim;

/**
 * What a status change submits: the reason, for a reject (ignored otherwise; null: none), and the token of the bulk
 * settle a settle is part of (ignored otherwise; null: taken one at a time).
 */
public record ClaimActionRequest(String reason, String batch) {
}
