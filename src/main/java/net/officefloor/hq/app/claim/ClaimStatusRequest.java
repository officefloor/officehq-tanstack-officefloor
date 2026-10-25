package net.officefloor.hq.app.claim;

/** The status a claim is to move to (OPEN, SETTLED or VOID), and the signed-in user (`by`) making the change. */
public record ClaimStatusRequest(String status, String by) {
}
