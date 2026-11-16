package net.officefloor.hq.app.person;

/** A merge once checked: the person merged (who leaves the sheet), the person they merge into, and who merges them. */
public record PersonMerge(Person from, Person into, String by) {
}
