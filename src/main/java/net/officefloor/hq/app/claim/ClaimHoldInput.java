package net.officefloor.hq.app.claim;

/** A validated hold or release of a claim: who is doing it. */
public record ClaimHoldInput(String user) {
}
