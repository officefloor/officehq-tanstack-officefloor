package net.officefloor.hq.app.person;

/** Body of a hold: its from-date (blank: the sheet's today) and its to-date (blank: none). */
public record HoldRequest(String from, String to) {
}
