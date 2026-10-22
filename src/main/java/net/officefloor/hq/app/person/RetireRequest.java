package net.officefloor.hq.app.person;

/** Body of retiring a person: who is retiring them, and the date they are retired from (blank: the sheet's today). */
public record RetireRequest(String user, String from) {
}
