package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheet}/people/{person}/retire: the date retired from (blank for the sheet's today). */
public record RetireRequest(String from) {
}
