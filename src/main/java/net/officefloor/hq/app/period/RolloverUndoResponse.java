package net.officefloor.hq.app.period;

/** The rollover undone: its old fiscal year (FY<yyyy>) and how many periods it removed. */
public record RolloverUndoResponse(String fy, int removed) {
}
