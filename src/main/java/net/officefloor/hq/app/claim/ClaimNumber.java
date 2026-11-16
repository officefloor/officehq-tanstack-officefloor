package net.officefloor.hq.app.claim;

/** The number a status change's audit line carries: the claim's number (a voided claim keeps it). */
public record ClaimNumber(Long value) {
}
