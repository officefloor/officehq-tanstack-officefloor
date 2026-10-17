package net.officefloor.hq.app.person;

/**
 * The body of adding or editing a person: a name (required) and a grade (A, B or C). An edit also
 * names the version of the person it was made against.
 */
public record PersonRequest(String name, String grade, Long version) {
}
