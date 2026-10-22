package net.officefloor.hq.app.rate;

/** Body of a rate set: who is making it, the percent, an optional from-date and an optional
 *  to-date (yyyy-mm-dd). */
public record RateRequest(String user, String rate, String from, String to) {
}
