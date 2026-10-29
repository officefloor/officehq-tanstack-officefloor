package net.officefloor.hq.app.period;

/** An undone rollover: the old fiscal year's name and how many periods it removed. */
public record RolloverUndoneResponse(String fy, int removed) {
}
