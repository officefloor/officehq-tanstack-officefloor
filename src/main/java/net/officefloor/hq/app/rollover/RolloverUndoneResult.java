package net.officefloor.hq.app.rollover;

/** An undone rollover: its old fiscal year and how many periods it removed. */
public record RolloverUndoneResult(String fy, int removed) {
}
