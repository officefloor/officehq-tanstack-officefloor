package net.officefloor.hq.app.claim;

/** The signed-in user (`by`) opening or cancelling a claim's editor. */
public record ClaimLockRequest(String by) {
}
