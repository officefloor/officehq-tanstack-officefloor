package net.officefloor.hq.app.claim;

/** The number a status change concerns: the one just given (settle, approve), the one the claim has (void, re-settle), or null. */
public record ClaimNumber(String value) {
}
