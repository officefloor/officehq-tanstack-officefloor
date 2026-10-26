package net.officefloor.hq.app.claim;

/** The body of a reassignment: the id of the person on the sheet the claim moves to. */
public record ReassignRequest(Long personId) {
}
