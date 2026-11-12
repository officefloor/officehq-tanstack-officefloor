package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/periods: the name, the from and to dates (yyyy-MM-dd, inclusive)
 *  and who adds it. */
public record PeriodRequest(String name, String from, String to, String user) {
}
