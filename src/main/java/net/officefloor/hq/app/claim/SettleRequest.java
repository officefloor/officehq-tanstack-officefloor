package net.officefloor.hq.app.claim;

/** A settle as the screen sends it: the date (yyyy-MM-dd) to settle the claim as of; blank for today. */
public record SettleRequest(String on) {
}
