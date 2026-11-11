package net.officefloor.hq.app.claim;

/** How many of the selected claims were voided, and how many were refused and left as they were. */
public record BulkVoidResponse(int voided, int refused) {
}
