package net.officefloor.hq.app.rate;

/** The body of an import of rates, as typed: lines of grade,rate,from,to (the last two may be absent or blank). */
public record RateImportRequest(String text) {
}
