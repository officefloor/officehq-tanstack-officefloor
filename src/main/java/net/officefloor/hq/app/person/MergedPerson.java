package net.officefloor.hq.app.person;

/** A merge done: the person merged ({@code from}, now gone from the sheet) and the person they went into. */
public record MergedPerson(Person from, Person into) {
}
