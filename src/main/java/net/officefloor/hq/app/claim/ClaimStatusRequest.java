package net.officefloor.hq.app.claim;

/** Body of a status change of a claim: the action (open, settle or void), and who takes it. */
public record ClaimStatusRequest(String action, String by) {
}
