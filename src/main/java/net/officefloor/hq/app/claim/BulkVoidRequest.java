package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk void: the ids of the selected claims, the void reason chosen (empty for none), and who voids them. */
public record BulkVoidRequest(List<Long> claimIds, String reason, String by) {
}
