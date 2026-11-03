package net.officefloor.hq.app.person;

/** One of a person's snapshots: the day being left (yyyy-mm-dd; null when the sheet had no today) and their
 *  due-total as it stood then (two decimals, or 'n/a'). */
public record SnapshotLine(String day, String dueTotal) {
}
