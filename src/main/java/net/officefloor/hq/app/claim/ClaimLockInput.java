package net.officefloor.hq.app.claim;

/** A validated take or release of a claim's editor lock: who is taking or releasing it. */
public record ClaimLockInput(String user) {
}
