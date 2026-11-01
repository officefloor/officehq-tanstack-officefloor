package net.officefloor.hq.app.person;

/** Body of a flag, as typed: its reason, the date it is in force until (inclusive) and who makes the change. */
public record FlagRequest(String reason, String until, String user) {
}
