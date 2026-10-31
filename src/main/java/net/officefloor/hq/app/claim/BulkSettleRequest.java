package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk settle: the selected claims' ids and who settles them. */
public record BulkSettleRequest(List<Long> claimIds, String user) {
}
