package net.officefloor.hq.app.person;

/** A hold to add: its from-date (blank: the sheet's today), its to-date (blank: open-ended), and the signed-in user (`by`) adding it. */
public record HoldRequest(String from, String to, String by) {
}
