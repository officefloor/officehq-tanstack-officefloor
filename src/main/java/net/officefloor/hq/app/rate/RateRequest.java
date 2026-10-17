package net.officefloor.hq.app.rate;

/** The body of setting a grade's rate: the grade (A, B or C) and a percent (0.00 or more, up to two decimals). */
public record RateRequest(String grade, String rate) {
}
