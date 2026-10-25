package net.officefloor.hq.app.person;

/** A person's fields, and the signed-in user (`by`) making the change. */
public record PersonRequest(String name, String grade, String by) {
}
