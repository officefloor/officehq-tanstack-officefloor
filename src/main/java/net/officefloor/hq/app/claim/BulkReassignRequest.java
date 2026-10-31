package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk reassignment: the selected claims' ids, the id of the person they move to, and who moves them. */
public record BulkReassignRequest(List<Long> claimIds, Long toPersonId, String user) {
}
