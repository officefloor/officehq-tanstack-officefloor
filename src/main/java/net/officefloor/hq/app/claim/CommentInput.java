package net.officefloor.hq.app.claim;

/** A validated comment on a claim: who is writing it and its text (trimmed, not empty). */
public record CommentInput(String user, String text) {
}
