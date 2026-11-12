package net.officefloor.hq.app.rate;

/** Body of POST /api/sheets/{sheetId}/rates: the grade, its percent and who sets it. */
public record RateRequest(String grade, String pct, String user) {
}
