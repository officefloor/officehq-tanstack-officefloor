package net.officefloor.hq.app.claim;

import java.util.List;

/** What a bulk settle submits: the ticked claims (in list order) and its batch token. */
public record BulkSettleRequest(List<Long> claimIds, String batch) {
}
