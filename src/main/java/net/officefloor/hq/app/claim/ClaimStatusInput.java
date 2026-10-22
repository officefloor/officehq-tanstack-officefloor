package net.officefloor.hq.app.claim;

/** A validated status change: who is making it and the status the claim is to have. */
public record ClaimStatusInput(String user, ClaimStatus to) {
}
