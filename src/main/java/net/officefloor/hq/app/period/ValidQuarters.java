package net.officefloor.hq.app.period;

/** A generate-quarters request that passed validation: a calendar year. */
public record ValidQuarters(int year, String user) {
}
