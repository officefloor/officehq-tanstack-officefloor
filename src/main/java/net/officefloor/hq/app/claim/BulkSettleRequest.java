package net.officefloor.hq.app.claim;

import java.util.List;

/** The bulk settle: the ids of the selected claims. */
public record BulkSettleRequest(List<Long> claimIds) {
}
