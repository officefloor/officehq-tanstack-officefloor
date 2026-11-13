package net.officefloor.hq.app.person;

/** A person's merge: the person merged ({@code from}) and the person they are merged into. */
public record Merge(Person from, Person into) {
}
