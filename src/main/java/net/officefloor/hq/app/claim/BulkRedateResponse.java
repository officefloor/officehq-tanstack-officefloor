package net.officefloor.hq.app.claim;

/** How many of the selected claims were re-dated and how many were refused. */
public record BulkRedateResponse(int redated, int refused) {
}
