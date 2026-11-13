package net.officefloor.hq.app.claim;

/** The number a status change concerns: the one just given (open), the one the claim has (void, settle), or null. */
public record ClaimNumber(Integer value) {
}
