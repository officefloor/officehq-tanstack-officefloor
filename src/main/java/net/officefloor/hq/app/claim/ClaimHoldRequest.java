package net.officefloor.hq.app.claim;

/** Body of holding or releasing a claim: who toggles it. */
public record ClaimHoldRequest(String user) {
}
