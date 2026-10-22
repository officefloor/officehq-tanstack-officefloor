package net.officefloor.hq.app.claim;

import java.util.Set;

/** A validated bulk reassign: who is moving, the selected claim ids and the person they move to. */
public record BulkReassignInput(String user, Set<Long> claimIds, long personId) {
}
