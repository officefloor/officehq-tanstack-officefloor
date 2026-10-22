package net.officefloor.hq.app.period;

/** Body of a period add: who is making it, the name, and the from and to dates (yyyy-mm-dd). */
public record PeriodRequest(String user, String name, String from, String to) {
}
