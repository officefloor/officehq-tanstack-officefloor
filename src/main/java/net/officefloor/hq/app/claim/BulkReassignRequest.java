package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk move: the ids of the selected claims, the person they move to, and who moves them. */
public record BulkReassignRequest(List<Long> claimIds, Long personId, String by) {
}
