package net.officefloor.hq.app.claim;

/** A deferral as the screen sends it: the date (yyyy-MM-dd) to defer the claim until. */
public record DeferRequest(String until) {
}
