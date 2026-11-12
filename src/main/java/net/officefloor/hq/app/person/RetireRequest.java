package net.officefloor.hq.app.person;

/** Body of a retirement, as typed: the date retired from (blank for the sheet's today) and who retires them. */
public record RetireRequest(String date, String user) {
}
