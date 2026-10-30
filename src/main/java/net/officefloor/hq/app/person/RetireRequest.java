package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/retire: the date retired from (blank takes
 *  the sheet's today) and who retires them. */
public record RetireRequest(String date, String user) {
}
