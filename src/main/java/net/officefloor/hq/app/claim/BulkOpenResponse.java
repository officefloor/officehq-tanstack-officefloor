package net.officefloor.hq.app.claim;

/** How many of the selected claims were opened and how many were refused. */
public record BulkOpenResponse(int opened, int refused) {
}
