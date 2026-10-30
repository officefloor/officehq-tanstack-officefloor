package net.officefloor.hq.app.person;

/** A merge undone: the person restored and the person they had been merged into. */
public record MergeUndone(Person restored, Person from) {
}
