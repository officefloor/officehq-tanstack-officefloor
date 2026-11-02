package net.officefloor.hq.app.person;

/** One of a person's patrons: another person on the same sheet, by id. */
public record Patron(long personId, long patronId) {
}
