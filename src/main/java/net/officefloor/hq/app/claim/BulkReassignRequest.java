package net.officefloor.hq.app.claim;

import java.util.List;

/** The selected claims' ids to move together, the person to move them to, and the signed-in user (`by`) moving them. */
public record BulkReassignRequest(List<Long> ids, Long personId, String by) {
}
