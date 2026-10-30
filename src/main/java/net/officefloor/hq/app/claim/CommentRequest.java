package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/comment: who comments the claim, and the comment. */
public record CommentRequest(String text, String user) {
}
