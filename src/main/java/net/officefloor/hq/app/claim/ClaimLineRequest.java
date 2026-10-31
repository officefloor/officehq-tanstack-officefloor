package net.officefloor.hq.app.claim;

/** Body of adding a line to a claim, as typed: the label, the quantity, the unit amount and who adds it. */
public record ClaimLineRequest(String label, String qty, String unit, String user) {
}
