package net.officefloor.hq.app.claim;

import java.util.List;

/** The bulk void: the ids of the selected claims and the void reason they all take (blank when none). */
public record BulkVoidRequest(List<Long> claimIds, String reason) {
}
