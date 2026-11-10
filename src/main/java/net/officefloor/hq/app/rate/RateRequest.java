package net.officefloor.hq.app.rate;

/** A rate as the screen sends it: a grade (A, B or C) and a percent with up to two decimals. */
public record RateRequest(String grade, String pct) {
}
