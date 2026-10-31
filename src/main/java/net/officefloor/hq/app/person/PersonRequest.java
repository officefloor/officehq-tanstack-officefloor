package net.officefloor.hq.app.person;

/** Body of a person add or edit: the name, the grade (A, B or C) and who makes the change. */
public record PersonRequest(String name, String grade, String user) {
}
