package net.officefloor.hq.app.rate;

/** The rate form as submitted: a grade and its percent. */
public record RateRequest(String grade, String pct) {
}
