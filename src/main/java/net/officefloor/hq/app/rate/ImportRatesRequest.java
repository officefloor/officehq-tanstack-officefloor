package net.officefloor.hq.app.rate;

/** Body of a rates import: lines of grade,rate,from,to as typed, and who sets them. */
public record ImportRatesRequest(String text, String user) {
}
