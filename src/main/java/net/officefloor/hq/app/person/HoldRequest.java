package net.officefloor.hq.app.person;

/** Body of holding a person: the from-date (blank: the sheet's today), the optional to-date, and who holds them. */
public record HoldRequest(String from, String to, String by) {
}
