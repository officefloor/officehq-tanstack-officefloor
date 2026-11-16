package net.officefloor.hq.app.sheet;

/** A SNAPSHOT written for the person {@code personId}: the day being left (yyyy-MM-dd) and their due-total on it (two
 *  decimals, or n/a). */
public record Snapshot(long personId, String day, String dueTotal) {
}
