package net.officefloor.hq.app.claim;

/** The outcome of a bulk void: how many of the selected claims were voided and how many refused. */
public record BulkVoidResult(int voided, int refused) {
}
