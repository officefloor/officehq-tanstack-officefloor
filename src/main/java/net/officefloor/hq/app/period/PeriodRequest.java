package net.officefloor.hq.app.period;

/** A period's name, its from and to dates, and the signed-in user (`by`) adding it. */
public record PeriodRequest(String name, String from, String to, String by) {
}
