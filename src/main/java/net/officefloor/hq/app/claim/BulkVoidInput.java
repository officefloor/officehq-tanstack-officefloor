package net.officefloor.hq.app.claim;

import java.util.Set;

/** A validated bulk void: who is voiding, the selected claim ids and the void reason (null: none). */
public record BulkVoidInput(String user, Set<Long> claimIds, String reason) {
}
