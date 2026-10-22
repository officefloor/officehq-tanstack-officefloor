package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk settle: who is settling and the ids of the selected claims. */
public record BulkSettleRequest(String user, List<Long> claimIds) {
}
