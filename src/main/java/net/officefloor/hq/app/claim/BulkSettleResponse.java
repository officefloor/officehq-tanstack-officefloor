package net.officefloor.hq.app.claim;

/** How many of the selected claims were settled (or left PENDING), and how many were refused and left as they were. */
public record BulkSettleResponse(int settled, int refused) {
}
