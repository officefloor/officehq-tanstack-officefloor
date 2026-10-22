package net.officefloor.hq.app.claim;

/** Body of a status change: who is making it and the action (open, settle or void). */
public record ClaimStatusRequest(String user, String action) {
}
