package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of POST /api/sheets/{sheet}/claims/bulk-open: the ids of the selected claims. */
public record BulkOpenRequest(List<Long> claims) {
}
