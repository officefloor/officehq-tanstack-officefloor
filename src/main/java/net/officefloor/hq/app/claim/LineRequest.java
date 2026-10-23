package net.officefloor.hq.app.claim;

/** Body of a line added to a claim: who is adding it, its label, quantity and unit amount, as typed. */
public record LineRequest(String user, String label, String qty, String unit) {
}
