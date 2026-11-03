package net.officefloor.hq.app.claim;

import java.util.List;

/** The bulk reassign: the ids of the selected claims and the person on the sheet they move to. */
public record BulkReassignRequest(List<Long> claimIds, Long personId) {
}
