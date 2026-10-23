package net.officefloor.hq.app.rollup;

/** A snapshot of a person: the day being left and their due-total on it, as the screen showed it. */
public record Snapshot(long personId, String day, String dueTotal) {
}
