package net.officefloor.hq.app.snapshot;

/** One of {@code person}'s snapshot lines: the day being left (yyyy-mm-dd) and their due-total on it. */
public record SnapshotLine(long person, String day, String dueTotal) {
}
