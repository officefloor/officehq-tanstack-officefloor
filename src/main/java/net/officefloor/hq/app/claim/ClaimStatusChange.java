package net.officefloor.hq.app.claim;

/** A status change once validated: the status it moves the claim to, and who makes it. */
public record ClaimStatusChange(ClaimStatus to, String by) {
}
