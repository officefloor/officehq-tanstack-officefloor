package net.officefloor.hq.app.rate;

/** Body of a rate set: who is making it, and the percent. */
public record RateRequest(String user, String rate) {
}
