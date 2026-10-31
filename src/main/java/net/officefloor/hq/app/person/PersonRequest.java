package net.officefloor.hq.app.person;

/** Body of a person add or edit: the name, the grade (A, B or C), who makes the change and, for an
 *  edit, the version of the person the editor loaded (null: no check). */
public record PersonRequest(String name, String grade, String user, Integer version) {
}
