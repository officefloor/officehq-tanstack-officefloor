package net.officefloor.hq.app.period;

/** Body of POST /api/sheets/{sheetId}/periods: the name, the from and to dates (yyyy-MM-dd, inclusive),
 *  who adds it and the cap on each person's open claims in it (a whole number of at least 1; blank for none). */
public record PeriodRequest(String name, String from, String to, String user, String cap) {
}
