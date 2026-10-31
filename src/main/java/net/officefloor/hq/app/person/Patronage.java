package net.officefloor.hq.app.person;

/** A patron just named for a person, and by whom. */
public record Patronage(Person person, Person patron, String user) {
}
