package net.officefloor.hq.app.person;

/** A flag as the screen sends it: its reason and until-date (blank: the sheet's today). */
public record FlagRequest(String reason, String until) {
}
