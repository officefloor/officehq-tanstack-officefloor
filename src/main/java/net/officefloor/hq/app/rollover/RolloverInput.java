package net.officefloor.hq.app.rollover;

/** A validated rollover: who is making it, and the calendar year the old fiscal year's first day falls in. */
public record RolloverInput(String user, int year) {
}
