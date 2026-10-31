package net.officefloor.hq.app.claim;

/** How many of the selected claims a bulk void voided and how many it refused. */
public record BulkVoidResponse(int voided, int refused) {
}
