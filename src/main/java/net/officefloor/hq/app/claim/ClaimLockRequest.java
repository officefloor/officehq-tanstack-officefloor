package net.officefloor.hq.app.claim;

/** Body of taking or releasing a claim's editor lock: who is taking or releasing it. */
public record ClaimLockRequest(String user) {
}
