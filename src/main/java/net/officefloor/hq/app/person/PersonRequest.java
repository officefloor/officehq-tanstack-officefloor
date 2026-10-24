package net.officefloor.hq.app.person;

/** The person form as submitted: a name and a grade. */
public record PersonRequest(String name, String grade) {
}
