package net.officefloor.hq.app.claim;

/** An assignment of a REVIEW claim once its body is read: the user it is assigned to (trimmed) and who assigns it. */
public record ClaimReviewAssignment(String user, String by) {
}
