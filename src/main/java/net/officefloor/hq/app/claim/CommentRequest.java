package net.officefloor.hq.app.claim;

/** Body of a comment on a claim: who is writing it and its text. */
public record CommentRequest(String user, String text) {
}
