package net.officefloor.hq.app.claim;

/** How many of the selected claims a bulk open opened, and how many it refused. */
public record BulkOpenResult(int opened, int refused) {
}
