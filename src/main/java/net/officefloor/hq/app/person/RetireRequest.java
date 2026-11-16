package net.officefloor.hq.app.person;

/** Body of retiring a person: the date they are retired from (blank: the sheet's today), and who retires them. */
public record RetireRequest(String from, String by) {
}
