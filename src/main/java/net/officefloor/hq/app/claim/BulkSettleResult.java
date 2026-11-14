package net.officefloor.hq.app.claim;

/** How many of the selected claims were settled (a claim gone to PENDING counts), and how many were
 *  refused (left as they were). */
public record BulkSettleResult(int settled, int refused) {
}
