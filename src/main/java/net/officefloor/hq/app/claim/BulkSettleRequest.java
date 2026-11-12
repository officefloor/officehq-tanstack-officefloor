package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk settle, as sent: the ids of the selected claims and who settles them. */
public record BulkSettleRequest(List<String> claimIds, String user) {
}
