package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/periods: the period's name and its from and to dates
 *  (YYYY-MM-DD, both inclusive). */
public record PeriodRequest(String name, String from, String to, String user) {
}
