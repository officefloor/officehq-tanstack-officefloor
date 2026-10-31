package net.officefloor.hq.app.rate;

/** Body of a rate change: the grade (A, B or C), the percent, the from-date (yyyy-mm-dd, blank for
 *  none) and who makes the change. */
public record RateRequest(String grade, String pct, String from, String user) {
}
