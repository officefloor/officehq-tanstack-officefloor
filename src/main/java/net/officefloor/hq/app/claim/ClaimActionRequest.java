package net.officefloor.hq.app.claim;

/** A claim's status change: the reason, for a reject, and the date to defer until, for a defer, and the amount to split off, for a split (each ignored
 *  by the other actions). */
public record ClaimActionRequest(String reason, String until, String amount) {
}
