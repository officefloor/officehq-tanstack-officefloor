package net.officefloor.hq.app.period;

/** The period form as submitted: a name, and its from and to date (YYYY-MM-DD). */
public record PeriodRequest(String name, String from, String to) {
}
