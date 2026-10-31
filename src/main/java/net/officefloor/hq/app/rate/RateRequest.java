package net.officefloor.hq.app.rate;

/** Body of a rate change: the grade (A, B or C), the percent and who makes the change. */
public record RateRequest(String grade, String pct, String user) {
}
