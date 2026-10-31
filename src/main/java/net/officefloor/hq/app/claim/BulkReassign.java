package net.officefloor.hq.app.claim;

import java.util.Set;

/** A validated bulk reassignment: the selected claims, the person they move to, and who moves them. */
public record BulkReassign(Set<Long> claimIds, long toPersonId, String user) {
}
