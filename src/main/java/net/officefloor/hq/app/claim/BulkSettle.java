package net.officefloor.hq.app.claim;

import java.util.Set;

/** A validated bulk settle: the selected claims and who settles them. */
public record BulkSettle(Set<Long> claimIds, String user) {
}
