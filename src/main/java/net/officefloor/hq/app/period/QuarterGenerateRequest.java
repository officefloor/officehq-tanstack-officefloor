package net.officefloor.hq.app.period;

/** Body of a quarters generate: who is making it, and the calendar year (yyyy) whose four quarters to add. */
public record QuarterGenerateRequest(String user, String year) {
}
