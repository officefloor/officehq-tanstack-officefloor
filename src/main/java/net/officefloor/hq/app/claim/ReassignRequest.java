package net.officefloor.hq.app.claim;

/** A claim's reassignment: the claim and the person on the sheet it moves to. */
public record ReassignRequest(Long claimId, Long personId) {
}
