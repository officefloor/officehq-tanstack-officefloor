package net.officefloor.hq.app.claim;

/** Body of opening or closing a claim's editor: who opens or closes it. */
public record ClaimLockRequest(String user) {
}
