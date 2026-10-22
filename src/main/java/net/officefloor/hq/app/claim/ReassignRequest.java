package net.officefloor.hq.app.claim;

/** Body of a reassign: who is making it and the id of the person the claim moves to. */
public record ReassignRequest(String user, String personId) {
}
