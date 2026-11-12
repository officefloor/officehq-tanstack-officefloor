package net.officefloor.hq.app.person;

/** Body of a flag, as typed: its reason, its until-date (blank for the sheet's today) and who flags the person. */
public record FlagRequest(String reason, String until, String user) {
}
