package net.officefloor.hq.app.claim;

/** A line to add to a claim: its label, quantity and unit amount, as typed. */
public record LineRequest(String label, String qty, String unit) {
}
