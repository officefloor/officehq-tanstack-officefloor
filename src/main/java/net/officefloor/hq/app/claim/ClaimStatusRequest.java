package net.officefloor.hq.app.claim;

/**
 * The body of a status change: {@code action} is "approve" or "reject" for a steward's decision on a PENDING claim,
 * or "review" for a steward's review of a REVIEW claim (to OPEN) (null for any other change), {@code reason} is why a rejected claim was rejected, and {@code voidReason} is the
 * reason from the sheet's list a claim is voided with (null or blank for none), and {@code settleOn} is the date (on or
 * before the sheet's today) a claim is settled as of (null or blank to settle it as of the sheet's today).
 */
public record ClaimStatusRequest(String action, String reason, String voidReason, String settleOn) {
}
