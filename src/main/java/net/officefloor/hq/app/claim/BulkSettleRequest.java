package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk settle: the ids of the selected claims, in any order. */
public record BulkSettleRequest(List<Long> claimIds) {
}
