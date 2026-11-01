package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheet}/people/{person}/hold: the from-date (blank for the sheet's today) and the optional to-date. */
public record HoldRequest(String from, String to) {
}
