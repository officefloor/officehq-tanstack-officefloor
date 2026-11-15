package net.officefloor.hq.app.person;

/** A SNAPSHOT written for a person: the day being left (yyyy-MM-dd) and their due-total on it (or n/a). */
public record Snapshot(String day, String dueTotal) {
}
