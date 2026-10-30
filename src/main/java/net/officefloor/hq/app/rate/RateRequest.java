package net.officefloor.hq.app.rate;

/** Body of POST /api/sheets/{sheetId}/rates: the grade, its percent ("12.50") and the optional date
 *  (YYYY-MM-DD) it applies from — blank for from the beginning of time — and the optional date it
 *  applies through — blank for no end. */
public record RateRequest(String grade, String rate, String from, String to, String user) {
}
