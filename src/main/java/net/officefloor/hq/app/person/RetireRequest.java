package net.officefloor.hq.app.person;

/** The body of retiring a person: the date retired from (yyyy-mm-dd; empty means the sheet's today). */
public record RetireRequest(String from) {
}
