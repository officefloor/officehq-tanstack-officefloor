package net.officefloor.hq.app.rate;

/** Body of POST /api/sheets/{sheet}/rates/import: lines of grade,rate,from,to (the last two may be absent or blank). */
public record RateImportRequest(String text) {
}
