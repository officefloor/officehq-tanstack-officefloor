package net.officefloor.hq.app.rate;

/** The rate form: the request body to set a grade's rate, with an optional from-date (yyyy-MM-dd). */
public record RateRequest(String pct, String from) {
}
