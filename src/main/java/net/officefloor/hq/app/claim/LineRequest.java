package net.officefloor.hq.app.claim;

/** Body of adding a line to a claim: its label, quantity and unit amount as typed, and who adds it. */
public record LineRequest(String label, String qty, String unit, String by) {
}
