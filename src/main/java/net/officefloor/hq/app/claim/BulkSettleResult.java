package net.officefloor.hq.app.claim;

/** How many of the selected claims a bulk settle settled (or sent to PENDING), and how many it refused. */
public record BulkSettleResult(int settled, int refused) {
}
