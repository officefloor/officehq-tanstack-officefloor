package net.officefloor.hq.app.rate;

/** A grade's rate, a percent shown with two decimals ("12.50"), and the date it applies from
 *  (YYYY-MM-DD; "" when it applies from the beginning of time) and through ("" when it has no end). */
public record RateResponse(String grade, String rate, String from, String to) {
}
