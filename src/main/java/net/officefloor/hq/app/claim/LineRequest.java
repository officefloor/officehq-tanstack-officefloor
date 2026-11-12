package net.officefloor.hq.app.claim;

/** Body of a line added to a claim, as sent: who adds it, its label, quantity and unit amount. */
public record LineRequest(String user, String label, String qty, String unit) {
}
