package net.officefloor.hq.app.rollover;

/** The rollover form as submitted: the fiscal year (yyyy) to roll over from, named by the calendar year its first day falls in. */
public record RolloverRequest(String year) {
}
