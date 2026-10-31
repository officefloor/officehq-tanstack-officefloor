package net.officefloor.hq.app.period;

/** Body of a new period: its name, its from and to dates (yyyy-mm-dd) and who adds it. */
public record PeriodRequest(String name, String from, String to, String user) {
}
