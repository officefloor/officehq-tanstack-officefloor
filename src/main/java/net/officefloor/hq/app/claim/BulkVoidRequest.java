package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk void: the ids of the selected claims, in any order, and the void reason (may be empty). */
public record BulkVoidRequest(List<Long> claimIds, String reason) {
}
