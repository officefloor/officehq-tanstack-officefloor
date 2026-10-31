package net.officefloor.hq.app.person;

/** Body of a retirement, as typed: the date retired from (blank for the sheet's today) and who
 *  makes the change. */
public record RetireRequest(String from, String user) {
}
