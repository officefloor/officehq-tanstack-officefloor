package net.officefloor.hq.app.person;

/** The body of holding a person: the from-date (yyyy-mm-dd; empty means the sheet's today) and the
 *  to-date (yyyy-mm-dd; empty means open-ended). */
public record HoldRequest(String from, String to) {
}
