package net.officefloor.hq.app.claim;

/** A claim's review being assigned by `user` to `assignee`. */
public record ClaimReviewAssign(String user, String assignee) {
}
