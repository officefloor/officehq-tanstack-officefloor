package net.officefloor.hq.app.claim;

/** Body of a claim status change: the action (open, settle or void) and who takes it. */
public record ClaimStatusRequest(String action, String user) {
}
