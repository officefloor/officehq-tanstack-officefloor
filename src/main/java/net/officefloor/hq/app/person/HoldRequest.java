package net.officefloor.hq.app.person;

/** The hold control: the from-date (blank: the sheet's today) and the optional to-date. */
public record HoldRequest(String from, String to) {
}
