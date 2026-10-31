package net.officefloor.hq.app.person;

/** Body of a hold, as typed: the date it is from (blank for the sheet's today), the date it is to
 *  (blank: open-ended) and who makes the change. */
public record HoldRequest(String from, String to, String user) {
}
