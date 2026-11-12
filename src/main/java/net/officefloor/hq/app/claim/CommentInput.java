package net.officefloor.hq.app.claim;

/** A validated comment: who writes it and its text, trimmed and not empty. */
public record CommentInput(String user, String text) {
}
