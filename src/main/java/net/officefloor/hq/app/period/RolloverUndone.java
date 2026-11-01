package net.officefloor.hq.app.period;

/** The rollover undone: the fiscal year it had rolled over, and how many periods were removed. */
public record RolloverUndone(int fy, int removed) {
}
