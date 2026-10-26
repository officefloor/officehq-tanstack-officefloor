package net.officefloor.hq.app.period;

/** A period's name, its from and to dates, its cap on each person's open claims (blank for none), and the signed-in user (`by`) adding it. */
public record PeriodRequest(String name, String from, String to, String by, String cap) {
}
