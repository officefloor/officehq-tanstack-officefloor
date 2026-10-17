package net.officefloor.hq.app.person;

/** The body of adding or editing a person: a name (required) and a grade (A, B or C). */
public record PersonRequest(String name, String grade) {
}
