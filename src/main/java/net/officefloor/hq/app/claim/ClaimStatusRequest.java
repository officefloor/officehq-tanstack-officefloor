package net.officefloor.hq.app.claim;

/** Body of a claim status change: the action (open, settle, approve, reject or void), who takes it
 *  and, for a rejection, its reason. */
public record ClaimStatusRequest(String action, String user, String reason) {
}
