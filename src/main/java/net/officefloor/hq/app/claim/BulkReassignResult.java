package net.officefloor.hq.app.claim;

/** How many of the selected claims a bulk reassignment moved, and how many it refused. */
public record BulkReassignResult(int reassigned, int refused) {
}
