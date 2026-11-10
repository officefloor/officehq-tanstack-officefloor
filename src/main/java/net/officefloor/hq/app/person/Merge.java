package net.officefloor.hq.app.person;

/** A person just merged into another: the person merged, and the person merged into. */
public record Merge(Person from, Person into) {
}
