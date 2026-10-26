package net.officefloor.hq.app.claim;

/**
 * The body of a status change: {@code action} is "approve" or "reject" for a steward's decision on a PENDING claim
 * (null for any other change), {@code reason} is why a rejected claim was rejected, and {@code voidReason} is the
 * reason from the sheet's list a claim is voided with (null or blank for none).
 */
public record ClaimStatusRequest(String action, String reason, String voidReason) {
}
