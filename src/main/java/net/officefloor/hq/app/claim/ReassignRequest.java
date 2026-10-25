package net.officefloor.hq.app.claim;

/** The person (by id) a claim is to move to, and the signed-in user (`by`) moving it. */
public record ReassignRequest(Long personId, String by) {
}
