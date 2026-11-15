package net.officefloor.hq.app.person;

/** A merge just made: the person merged and the one they were merged into, for the audit line. */
public record Merge(Person from, Person into) {
}
