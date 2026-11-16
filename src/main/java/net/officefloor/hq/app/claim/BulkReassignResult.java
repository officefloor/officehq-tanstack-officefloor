package net.officefloor.hq.app.claim;

/** The outcome of a bulk move: how many of the selected claims were moved and how many refused. */
public record BulkReassignResult(int reassigned, int refused) {
}
