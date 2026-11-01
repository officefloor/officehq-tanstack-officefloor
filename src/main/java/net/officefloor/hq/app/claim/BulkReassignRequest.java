package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of POST /api/sheets/{sheet}/claims/bulk-reassign: the ids of the selected claims and the person they move to. */
public record BulkReassignRequest(List<Long> claims, Long to) {
}
