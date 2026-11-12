package net.officefloor.hq.app.period;

/** A validated generate of quarters: the calendar year and who makes them. */
public record QuartersInput(int year, String user) {
}
