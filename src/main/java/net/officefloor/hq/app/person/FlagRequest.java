package net.officefloor.hq.app.person;

/** The flag form: the reason and the date it is in force until (yyyy-mm-dd). */
public record FlagRequest(String reason, String until) {
}
