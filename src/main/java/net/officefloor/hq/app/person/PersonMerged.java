package net.officefloor.hq.app.person;

/** A merge done: the merged person's name, and the person they were merged into. */
public record PersonMerged(String from, Person into) {
}
