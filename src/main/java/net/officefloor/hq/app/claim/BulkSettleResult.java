package net.officefloor.hq.app.claim;

/** How many of the selected claims a bulk settle settled (a claim gone PENDING counts), and how many it refused. */
public record BulkSettleResult(int settled, int refused) {
}
