package net.officefloor.hq.app.person;

/** Body of holding a person: who is doing it, the date the hold runs from (blank: the sheet's today) and the date
 *  it runs to (blank: open-ended). */
public record HoldRequest(String user, String from, String to) {
}
