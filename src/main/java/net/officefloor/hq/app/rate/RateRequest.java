package net.officefloor.hq.app.rate;

/**
 * A rate as the screen sends it: a grade (A, B or C), a percent with up to two decimals, and an
 * optional from-date (yyyy-MM-dd; blank for from the beginning of time) and an optional to-date
 * (yyyy-MM-dd; blank for no end).
 */
public record RateRequest(String grade, String pct, String from, String to) {
}
