package net.officefloor.hq.app.claim;

/** Body of a status change: who is making it, the action (open, settle, approve, reject or void) and, for a
 *  reject, the reason. */
public record ClaimStatusRequest(String user, String action, String reason) {
}
