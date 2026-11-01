package net.officefloor.hq.app.claim;

/** One comment on a claim: the user who wrote it, the sheet's today then (yyyy-MM-dd), and its text. */
public record ClaimComment(String by, String on, String text) {
}
