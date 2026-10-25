package net.officefloor.hq.app.rate;

/** A grade's rate (a percent), and the signed-in user (`by`) making the change. */
public record RateRequest(String grade, String rate, String by) {
}
