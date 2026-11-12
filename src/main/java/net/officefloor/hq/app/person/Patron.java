package net.officefloor.hq.app.person;

/** The person just named as a patron (wrapped, so it is told apart from the person they back). */
public record Patron(Person person) {
}
