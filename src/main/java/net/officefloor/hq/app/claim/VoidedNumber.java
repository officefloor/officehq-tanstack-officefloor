package net.officefloor.hq.app.claim;

/** The number a claim had before it was voided (null when it had none), for its audit line. */
public record VoidedNumber(Integer value) {
}
