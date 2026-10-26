package net.officefloor.hq.app.person;

/** The body of a hold: its from-date and to-date, yyyy-mm-dd; a blank from-date is the sheet's today, a blank to-date has no end. */
public record HoldRequest(String from, String to) {
}
