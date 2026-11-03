package net.officefloor.hq.app.person;

/** One of a person's patrons: another person on the same sheet, by id (listed in the order they were named). */
public record Patron(long personId, long patronId) {
}
