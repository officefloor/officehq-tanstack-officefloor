package net.officefloor.hq.app.person;

/** Body of an add or edit of a person, and who makes it. */
public record PersonRequest(String name, String grade, String by) {
}
