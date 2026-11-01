package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of POST /api/sheets/{sheet}/claims/bulk-settle: the ids of the selected claims. */
public record BulkSettleRequest(List<Long> claims) {
}
