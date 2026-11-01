package net.officefloor.hq.app.rate;

/** Body of POST /api/sheets/{sheet}/rates. */
public record RateRequest(String grade, String pct) {
}
