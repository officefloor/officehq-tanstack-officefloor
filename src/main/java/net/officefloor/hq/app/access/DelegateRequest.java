package net.officefloor.hq.app.access;

/** A user name to delegate the steward role to, its from-date (blank: the sheet's today) and to-date, and the signed-in user (`by`). */
public record DelegateRequest(String user, String from, String to, String by) {
}
