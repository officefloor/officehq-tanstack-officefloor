package net.officefloor.hq.app.person;

/** A retirement as the screen sends it: the date the person is retired from (blank: the sheet's today). */
public record RetireRequest(String from) {
}
