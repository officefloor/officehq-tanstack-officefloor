package net.officefloor.hq.app.claim;

/** What a reassign submits: the id of the person the claim moves to (null: none chosen). */
public record ReassignRequest(Long personId) {
}
