package net.officefloor.hq.app.claim;

/** A claim line as the screen sends it: its label, quantity and unit amount. */
public record LineRequest(String label, String qty, String unit) {
}
