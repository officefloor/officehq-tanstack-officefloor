package net.officefloor.hq.app.claim;

/** Body of assigning a claim's review: who assigns it, and the user it is assigned to. */
public record ClaimReviewAssignRequest(String user, String assignee) {
}
