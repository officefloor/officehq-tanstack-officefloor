package net.officefloor.hq.app.period;

/** The body of adding a period: its name, and its from and to dates (yyyy-mm-dd), inclusive. */
public record PeriodRequest(String name, String from, String to) {
}
