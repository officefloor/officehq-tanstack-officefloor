package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk reassign: who is moving, the ids of the selected claims and the id of the person they move to. */
public record BulkReassignRequest(String user, List<Long> claimIds, String personId) {
}
