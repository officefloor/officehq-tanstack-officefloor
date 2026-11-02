package net.officefloor.hq.app.person;

/** The hold form: the from-date (blank: the sheet's today) and the to-date (blank: open-ended). */
public record HoldRequest(String from, String to) {
}
