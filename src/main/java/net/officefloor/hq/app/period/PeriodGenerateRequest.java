package net.officefloor.hq.app.period;

/** Body of a period generate: who is making it, and the from and to dates (yyyy-mm-dd). */
public record PeriodGenerateRequest(String user, String from, String to) {
}
