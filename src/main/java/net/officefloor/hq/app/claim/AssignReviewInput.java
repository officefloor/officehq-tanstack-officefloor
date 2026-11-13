package net.officefloor.hq.app.claim;

/** A validated review assignment: the steward who assigns it and the user it is assigned to, trimmed. */
public record AssignReviewInput(String user, String assignee) {
}
