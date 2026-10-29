package net.officefloor.hq.app.flag;

/** The body of flagging a person: the reason and the until-date (yyyy-mm-dd; blank: the sheet's today), as typed. */
public record FlagRequest(String reason, String until) {
}
