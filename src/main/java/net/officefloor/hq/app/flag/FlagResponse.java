package net.officefloor.hq.app.flag;

/** A person's flag: the reason and the date it runs until (yyyy-mm-dd). */
public record FlagResponse(long personId, String reason, String until) {
}
