package net.officefloor.hq.app.claim;

/** The outcome of a bulk open: how many of the selected claims were opened and how many refused. */
public record BulkOpenResponse(int opened, int refused) {
}
