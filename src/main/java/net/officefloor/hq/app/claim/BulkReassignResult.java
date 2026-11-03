package net.officefloor.hq.app.claim;

/** How many of the selected claims were moved and how many were refused. */
public record BulkReassignResult(int reassigned, int refused) {
}
