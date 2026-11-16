package net.officefloor.hq.app.claim;

/** The number a status change's audit line carries: the claim's number, or for a voided claim the one it had. */
public record ClaimNumber(Long value) {
}
