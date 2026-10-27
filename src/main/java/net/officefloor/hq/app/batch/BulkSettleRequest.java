package net.officefloor.hq.app.batch;

import java.util.List;

/** The body of a bulk settle: the ids of the ticked claims, in list order. */
public record BulkSettleRequest(List<Long> claimIds) {
}
