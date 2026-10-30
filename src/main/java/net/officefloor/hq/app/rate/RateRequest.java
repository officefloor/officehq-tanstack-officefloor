package net.officefloor.hq.app.rate;

/** Body of POST /api/sheets/{sheetId}/rates: the grade, its percent ("12.50") and the optional date
 *  (YYYY-MM-DD) it applies from — blank for from the beginning of time. */
public record RateRequest(String grade, String rate, String from, String user) {
}
