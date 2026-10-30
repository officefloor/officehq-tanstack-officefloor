package net.officefloor.hq.app.period;

/** A period: its name and its from and to dates (YYYY-MM-DD, both inclusive). */
public record PeriodResponse(String name, String from, String to) {
}
