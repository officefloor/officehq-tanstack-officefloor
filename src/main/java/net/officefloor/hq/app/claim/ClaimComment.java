package net.officefloor.hq.app.claim;

/** A validated comment on a claim: its text (trimmed, never empty) and who writes it. */
public record ClaimComment(String text, String user) {
}
