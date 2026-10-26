package net.officefloor.hq.app.rate;

/**
 * The body of a rate being set: the percent as typed, the from-date as typed (blank: from the beginning of time) and
 * the to-date as typed (blank: with no end).
 */
public record RateRequest(String rate, String from, String to) {
}
