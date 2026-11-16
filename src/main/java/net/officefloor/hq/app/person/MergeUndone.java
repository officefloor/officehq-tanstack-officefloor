package net.officefloor.hq.app.person;

/** A merge just undone: the person restored and the one they had been merged into, for the audit line. */
public record MergeUndone(Person restored, Person from) {
}
