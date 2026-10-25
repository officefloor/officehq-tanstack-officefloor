package net.officefloor.hq.app.claim;

import java.util.Set;

/** The claims selected to be moved together, and the id of the person they move to. */
public record BulkReassignIds(Set<Long> ids, long personId) {
}
