package net.officefloor.hq.app.claim;

/**
 * The body of a status change: {@code action} is "approve" or "reject" for a steward's decision on a PENDING claim
 * (null for any other change), and {@code reason} is why a rejected claim was rejected.
 */
public record ClaimStatusRequest(String action, String reason) {
}
