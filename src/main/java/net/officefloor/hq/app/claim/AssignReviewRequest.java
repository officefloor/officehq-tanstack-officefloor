package net.officefloor.hq.app.claim;

/** Body of a review assignment, as sent: who assigns it, and the user it is assigned to. */
public record AssignReviewRequest(String user, String assignee) {
}
