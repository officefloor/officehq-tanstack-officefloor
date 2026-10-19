package net.officefloor.hq.app.person;

/** The person form: the request body to add or edit a person. */
public record PersonRequest(String name, String grade) {
}
