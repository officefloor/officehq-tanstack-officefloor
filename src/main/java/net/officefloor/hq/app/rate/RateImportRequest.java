package net.officefloor.hq.app.rate;

/** Body of an import of rates, as typed: lines of grade,rate,from,to and who makes the change. */
public record RateImportRequest(String text, String user) {
}
