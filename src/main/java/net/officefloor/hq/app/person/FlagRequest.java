package net.officefloor.hq.app.person;

/** Body of a flag: its reason and its until-date (blank: the sheet's today). */
public record FlagRequest(String reason, String until) {
}
