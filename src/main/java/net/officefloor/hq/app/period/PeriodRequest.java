package net.officefloor.hq.app.period;

/** The body of a period being added: the name and the from and to dates as typed. */
public record PeriodRequest(String name, String from, String to) {
}
