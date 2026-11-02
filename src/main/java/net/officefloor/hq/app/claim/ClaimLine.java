package net.officefloor.hq.app.claim;

/** One line of a claim: its label, its quantity (a whole number, at least 1) and its unit amount (two decimals). */
public record ClaimLine(String label, int qty, String unit) {
}
