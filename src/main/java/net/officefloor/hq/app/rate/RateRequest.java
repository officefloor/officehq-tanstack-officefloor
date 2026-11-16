package net.officefloor.hq.app.rate;

/** Body of setting a grade's rate: the percent as text, and who sets it. */
public record RateRequest(String rate, String by) {
}
