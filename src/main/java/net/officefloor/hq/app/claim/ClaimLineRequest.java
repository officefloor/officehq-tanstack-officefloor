package net.officefloor.hq.app.claim;

/** The body of a line added to a claim, as typed: its label, its quantity (a whole number) and its unit amount. */
public record ClaimLineRequest(String label, String qty, String unit) {
}
