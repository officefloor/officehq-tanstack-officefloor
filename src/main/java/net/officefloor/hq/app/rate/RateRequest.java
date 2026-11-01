package net.officefloor.hq.app.rate;

/** Body of POST /api/sheets/{sheet}/rates; a blank from-date sets the rate that applies from the start, a blank to-date one without end. */
public record RateRequest(String grade, String pct, String from, String to) {
}
