package net.officefloor.hq.app.rollover;

/** A rollover undone: its old fiscal year and how many periods were removed. */
public record RolloverUndoResponse(String fy, int removed) {
}
