package net.officefloor.hq.app.claim;

/** The body of moving a claim to another person: the id of the person on the sheet it moves to. */
public record ReassignRequest(Long personId) {
}
