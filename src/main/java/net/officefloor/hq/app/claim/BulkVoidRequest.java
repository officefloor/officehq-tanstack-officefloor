package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk void: the selected claims' ids, who voids them and the void reason (if any). */
public record BulkVoidRequest(List<Long> claimIds, String user, String reason) {
}
