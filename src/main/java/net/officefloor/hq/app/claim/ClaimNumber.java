package net.officefloor.hq.app.claim;

/** The number a claim had before its status changed (null when it had none): a voided claim's
 *  audit line carries the number voiding cleared. */
public record ClaimNumber(Long value) {
}
