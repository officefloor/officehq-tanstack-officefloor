package net.officefloor.hq.app.claim;

/**
 * The status a claim is to move to (OPEN, SETTLED or VOID), and the signed-in user (`by`) making the change. With
 * `review`, a steward's review of a PENDING claim: SETTLED approves it, OPEN rejects it with the `reason`. A void
 * carries the `voidReason` chosen from the sheet's list (blank: none).
 */
public record ClaimStatusRequest(String status, String by, Boolean review, String reason, String voidReason) {
}
