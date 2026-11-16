package net.officefloor.hq.app.claim;

/** The outcome of a bulk re-date: how many of the selected claims were re-dated and how many refused. */
public record BulkRedateResult(int redated, int refused) {
}
