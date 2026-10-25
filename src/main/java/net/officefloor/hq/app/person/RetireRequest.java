package net.officefloor.hq.app.person;

/** The date to retire a person from (blank: the sheet's today), and the signed-in user (`by`) retiring them. */
public record RetireRequest(String date, String by) {
}
