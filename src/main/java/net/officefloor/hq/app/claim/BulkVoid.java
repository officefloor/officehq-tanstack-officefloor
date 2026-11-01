package net.officefloor.hq.app.claim;

import java.util.Set;

/** A validated bulk void: the selected claims, who voids them and the void reason (null when none chosen). */
public record BulkVoid(Set<Long> claimIds, String user, String reason) {
}
