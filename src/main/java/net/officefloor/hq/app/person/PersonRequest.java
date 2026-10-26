package net.officefloor.hq.app.person;

/** The body of an add or an edit of a person: the name as typed and the grade (A, B or C). */
public record PersonRequest(String name, String grade) {
}
