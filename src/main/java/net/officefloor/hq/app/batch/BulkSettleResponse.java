package net.officefloor.hq.app.batch;

/** What a bulk settle did: how many claims it settled (to SETTLED or PENDING), how many it refused, and the sheet's last batch number. */
public record BulkSettleResponse(int settled, int refused, Integer last) {
}
