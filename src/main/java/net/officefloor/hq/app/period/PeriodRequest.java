package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/periods: the period's name and its from and to dates
 *  (YYYY-MM-DD, both inclusive), and its cap on each person's OPEN or PENDING claims (a whole number of at least 1,
 *  blank for none). */
public record PeriodRequest(String name, String from, String to, String user, String cap) {
}
