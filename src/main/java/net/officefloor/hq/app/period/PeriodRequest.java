package net.officefloor.hq.app.period;

/** The body of a period being added: the name, the from and to dates and the claim cap (blank: none) as typed. */
public record PeriodRequest(String name, String from, String to, String cap) {
}
