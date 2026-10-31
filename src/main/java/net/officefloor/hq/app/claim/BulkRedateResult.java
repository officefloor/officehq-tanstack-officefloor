package net.officefloor.hq.app.claim;

/** How many of the selected claims a bulk re-dating re-dated, and how many it refused. */
public record BulkRedateResult(int redated, int refused) {
}
