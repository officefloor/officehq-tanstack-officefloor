package net.officefloor.hq.app.rate;

/** Body of setting a grade's rate; a blank from-date applies from the beginning of time. */
public record RateRequest(String grade, String pct, String from) {
}
