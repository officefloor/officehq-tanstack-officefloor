package net.officefloor.hq.app.flag;

/** Body of flagging a person: who is doing it, the reason, and the date the flag runs until (yyyy-mm-dd). */
public record FlagRequest(String user, String reason, String until) {
}
