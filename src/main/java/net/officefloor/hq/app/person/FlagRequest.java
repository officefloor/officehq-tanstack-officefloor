package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheet}/people/{person}/flag: the reason and the until-date (blank for the sheet's today). */
public record FlagRequest(String reason, String until) {
}
