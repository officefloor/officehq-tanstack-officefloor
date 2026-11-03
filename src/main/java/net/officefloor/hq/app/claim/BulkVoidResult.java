package net.officefloor.hq.app.claim;

/** How many of the selected claims were voided and how many were refused. */
public record BulkVoidResult(int voided, int refused) {
}
