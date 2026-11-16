package net.officefloor.hq.app.rate;

/** Body of an import of rates: lines of grade,rate,from,to, and who imports them. */
public record ImportRatesRequest(String text, String by) {
}
