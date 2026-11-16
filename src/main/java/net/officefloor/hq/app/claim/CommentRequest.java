package net.officefloor.hq.app.claim;

/** Body of commenting a claim: the comment's text and who writes it. */
public record CommentRequest(String text, String by) {
}
