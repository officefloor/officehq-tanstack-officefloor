package net.officefloor.hq.app.person;

/** The person form as submitted: a name and a grade, and for an edit the version of the person it loaded. */
public record PersonRequest(String name, String grade, Long version) {
}
