package net.officefloor.hq.app.claim;

/** A validated status change: who is making it, the action (open, settle, approve, reject or void), the status
 *  the claim is to have (a settle above the clerk limit makes it PENDING instead) and, for a reject, the reason
 *  (null otherwise). */
public record ClaimStatusInput(String user, String action, ClaimStatus to, String reason) {
}
