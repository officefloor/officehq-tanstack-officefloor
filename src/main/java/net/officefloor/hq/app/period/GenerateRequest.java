package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/periods/generate: the from-date and to-date (YYYY-MM-DD; blank is
 *  the sheet's today) whose months each become a period. */
public record GenerateRequest(String from, String to, String user) {
}
