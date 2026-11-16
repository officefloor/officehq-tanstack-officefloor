package net.officefloor.hq.app.claim;

/** Body of a status change of a claim: the action (open, settle, approve, reject or void), who takes it, and the
 *  reason a rejection gives. */
public record ClaimStatusRequest(String action, String by, String reason) {
}
