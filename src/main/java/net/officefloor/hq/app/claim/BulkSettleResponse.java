package net.officefloor.hq.app.claim;

/** How many of the selected claims were settled (including those sent to PENDING) and how many were refused. */
public record BulkSettleResponse(int settled, int refused) {
}
