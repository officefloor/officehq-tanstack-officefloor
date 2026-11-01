package net.officefloor.hq.app.person;

/** A merge just undone: the person restored, the person they had been merged into, and by whom. */
public record MergeUndone(Person restored, Person from, String user) {
}
