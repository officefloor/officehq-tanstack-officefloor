package net.officefloor.hq.app.person;

/**
 * Body of a person add or edit: the name, the grade (A, B or C) and who makes the change. An edit
 * also carries the version of the person its editor loaded (none for an add).
 */
public record PersonRequest(String name, String grade, String user, Long version) {
}
