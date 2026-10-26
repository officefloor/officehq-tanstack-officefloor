package net.officefloor.hq.app.rate;

/** The body of a rate being set: the percent as typed, and the from-date as typed (blank: from the beginning of time). */
public record RateRequest(String rate, String from) {
}
