package net.officefloor.hq.app.rate;

/** The body of setting a grade's rate: the grade (A, B or C) a percent (0.00 or more, up to two decimals)
 *  and an optional from-date (yyyy-mm-dd; empty or absent: from the beginning of time). */
public record RateRequest(String grade, String rate, String from) {
}
