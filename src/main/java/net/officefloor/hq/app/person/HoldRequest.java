package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/hold: the hold's from-date (blank: the
 *  sheet's today), its optional to-date (blank: open-ended) and who holds them. */
public record HoldRequest(String from, String to, String user) {
}
