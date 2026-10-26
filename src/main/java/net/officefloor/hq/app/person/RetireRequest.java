package net.officefloor.hq.app.person;

/** The body of a retirement: the date retired from, yyyy-mm-dd, blank for the sheet's today. */
public record RetireRequest(String from) {
}
