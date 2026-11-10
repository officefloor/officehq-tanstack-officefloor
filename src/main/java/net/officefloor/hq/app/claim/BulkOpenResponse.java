package net.officefloor.hq.app.claim;

/** How many of the selected claims were opened, and how many were refused and left as they were. */
public record BulkOpenResponse(int opened, int refused) {
}
