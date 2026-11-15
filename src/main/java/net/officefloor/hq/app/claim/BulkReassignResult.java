package net.officefloor.hq.app.claim;

/** How many of the selected claims were moved to the person, and how many were refused (left as they were). */
public record BulkReassignResult(int reassigned, int refused) {
}
