package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/periods/generate: the from and to dates (yyyy-MM-dd) and who
 *  generates the periods. */
public record GenerateRequest(String from, String to, String user) {
}
