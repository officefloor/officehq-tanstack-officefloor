package net.officefloor.hq.app.claim;

/** How many of the selected claims were settled (or sent to PENDING) and how many were refused. */
public record BulkSettleResult(int settled, int refused) {
}
