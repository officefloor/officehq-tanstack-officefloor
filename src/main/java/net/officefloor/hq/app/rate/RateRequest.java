package net.officefloor.hq.app.rate;

/** Body of a rate set: who is making it, the percent, and an optional from-date (yyyy-mm-dd). */
public record RateRequest(String user, String rate, String from) {
}
