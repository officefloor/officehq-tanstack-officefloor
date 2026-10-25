package net.officefloor.hq.app.claim;

/** The signed-in user (`by`) commenting a claim, and the comment's text. */
public record CommentRequest(String text, String by) {
}
