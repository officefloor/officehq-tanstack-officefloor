package net.officefloor.hq.app.rate;

/** Body of POST /api/sheets/{sheetId}/rates: the grade, its percent, the date it applies from (blank
 *  for from the beginning of time) and who sets it. */
public record RateRequest(String grade, String pct, String from, String user) {
}
