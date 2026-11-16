package net.officefloor.hq.app.claim;

/** Body of moving a claim to another person: the id of the person it moves to, and who moves it. */
public record ReassignRequest(Long personId, String by) {
}
