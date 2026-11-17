package net.officefloor.hq.app.person;

/** Body of flagging a person: the reason, the until-date (blank: the sheet's today), and who flags them. */
public record FlagRequest(String reason, String until, String by) {
}
