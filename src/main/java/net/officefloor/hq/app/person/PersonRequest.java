package net.officefloor.hq.app.person;

/** Body of a person add or edit: who is making it, and the person's name and grade. */
public record PersonRequest(String user, String name, String grade) {
}
