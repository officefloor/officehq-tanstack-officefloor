package net.officefloor.hq.app.claim;

/** How many of the selected claims were moved, and how many were refused and left as they were. */
public record BulkReassignResponse(int reassigned, int refused) {
}
