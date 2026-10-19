package net.officefloor.hq.app.period;

/** The period form: the request body to add a period, its dates as yyyy-MM-dd. */
public record PeriodRequest(String name, String from, String to) {
}
