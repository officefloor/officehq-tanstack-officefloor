package net.officefloor.hq.app.rate;

/** Body of POST /api/sheets/{sheetId}/rates: the grade and its percent ("12.50"). */
public record RateRequest(String grade, String rate, String user) {
}
