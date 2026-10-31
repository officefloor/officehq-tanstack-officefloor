package net.officefloor.hq.app.person;

/** A person just merged into another, and by whom. */
public record Merger(Person from, Person into, String user) {
}
