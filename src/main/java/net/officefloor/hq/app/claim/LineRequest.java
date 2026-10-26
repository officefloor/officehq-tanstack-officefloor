package net.officefloor.hq.app.claim;

/** The signed-in user (`by`) adding a line to a claim, and the line as typed: label, quantity and unit amount. */
public record LineRequest(String label, String qty, String unit, String by) {
}
