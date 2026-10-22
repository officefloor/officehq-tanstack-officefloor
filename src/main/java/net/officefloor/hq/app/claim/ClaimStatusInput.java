package net.officefloor.hq.app.claim;

/** A validated status change: who is making it, the action (open, settle, approve, reject, void or resettle), the status
 *  the claim is to have (a settle above the clerk limit makes it PENDING instead) and, for a reject, the reason, and for a void the
 *  reason chosen from the sheet's list of void reasons (null otherwise). */
public record ClaimStatusInput(String user, String action, ClaimStatus to, String reason) {
}
