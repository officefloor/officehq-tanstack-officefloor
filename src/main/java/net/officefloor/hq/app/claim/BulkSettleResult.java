package net.officefloor.hq.app.claim;

/** The outcome of a bulk settle: how many of the selected claims were settled (or made PENDING) and how many refused. */
public record BulkSettleResult(int settled, int refused) {
}
