package net.officefloor.hq.app.period;

/** Body of adding a period: its name, from and to dates (yyyy-MM-dd), and who adds it. */
public record PeriodRequest(String name, String from, String to, String by) {
}
