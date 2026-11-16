package net.officefloor.hq.app.rate;

/** Body of setting a grade's rate: the percent as text, its from-date (yyyy-MM-dd, blank for none), and who sets it. */
public record RateRequest(String rate, String from, String by) {
}
