package net.officefloor.hq.app.claim;

/** How many of the selected claims were opened and how many were refused. */
public record BulkOpenResult(int opened, int refused) {
}
