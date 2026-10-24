package net.officefloor.hq.app.period;

/** The period form as submitted: a name, its from and to date (YYYY-MM-DD), and its cap on open claims per person (blank for none). */
public record PeriodRequest(String name, String from, String to, String cap) {
}
