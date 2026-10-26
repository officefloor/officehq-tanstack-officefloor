package net.officefloor.hq.app.person;

/**
 * The body of an add or an edit of a person: the name as typed and the grade (A, B or C). An edit
 * also names the {@code version} its form loaded (null on an add).
 */
public record PersonRequest(String name, String grade, Long version) {
}
