package net.officefloor.hq.app.rate;

/** Body of POST /api/sheets/{sheetId}/import-rates: lines of grade,rate,from,to (the dates may be absent or blank). */
public record ImportRatesRequest(String text, String user) {
}
