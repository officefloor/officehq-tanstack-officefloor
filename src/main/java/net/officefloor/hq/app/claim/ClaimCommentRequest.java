package net.officefloor.hq.app.claim;

/** Body of commenting on a claim: the comment's text and who writes it. */
public record ClaimCommentRequest(String text, String user) {
}
