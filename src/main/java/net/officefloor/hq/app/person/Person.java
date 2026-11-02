package net.officefloor.hq.app.person;

/** A person on a sheet: the id the store assigned, their name and grade (A, B or C). */
public record Person(long id, String name, String grade) {
}
