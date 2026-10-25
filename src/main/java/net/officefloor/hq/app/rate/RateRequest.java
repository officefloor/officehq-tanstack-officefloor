package net.officefloor.hq.app.rate;

/** A grade's rate (a percent), its optional from- and to-dates, and the signed-in user (`by`) making the change. */
public record RateRequest(String grade, String rate, String from, String to, String by) {
}
