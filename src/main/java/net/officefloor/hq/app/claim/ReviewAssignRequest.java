package net.officefloor.hq.app.claim;

/** Body of a review assignment: who is making it, and the user to review the claim. */
public record ReviewAssignRequest(String user, String reviewer) {
}
