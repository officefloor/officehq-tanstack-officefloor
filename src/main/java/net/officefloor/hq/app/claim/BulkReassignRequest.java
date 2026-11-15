package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk reassign: the ids of the selected claims, in any order, and the person they move to. */
public record BulkReassignRequest(List<Long> claimIds, Long to) {
}
