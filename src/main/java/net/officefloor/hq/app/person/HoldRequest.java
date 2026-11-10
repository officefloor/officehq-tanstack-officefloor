package net.officefloor.hq.app.person;

/** A hold as the screen sends it: its from-date (blank: the sheet's today) and to-date (blank: none). */
public record HoldRequest(String from, String to) {
}
