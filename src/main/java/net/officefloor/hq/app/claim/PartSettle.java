package net.officefloor.hq.app.claim;

/** The status (SETTLED or PENDING) and fixed due a part-settle moves the split-off claim to. */
public record PartSettle(String status, String settledDue) {
}
