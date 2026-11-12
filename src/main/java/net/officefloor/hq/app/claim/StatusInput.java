package net.officefloor.hq.app.claim;

/** A validated claim status change; {@code review} for a steward approving (SETTLED) or rejecting
 *  (OPEN, with its trimmed {@code reason}) a PENDING claim, or reviewing (OPEN) a REVIEW claim. */
public record StatusInput(ClaimStatus status, String user, boolean review, String reason) {
}
