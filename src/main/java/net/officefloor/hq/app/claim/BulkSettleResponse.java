package net.officefloor.hq.app.claim;

/**
 * How many of the selected claims were settled (a claim sent to PENDING counts as settled), and how
 * many were refused and left as they were.
 */
public record BulkSettleResponse(int settled, int refused) {
}
