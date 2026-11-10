package net.officefloor.hq.app.person;

/**
 * A person as the screen sends it, to add or to edit: a name and a grade (A, B or C). An edit
 * also names the version of the person it was made from.
 */
public record PersonRequest(String name, String grade, Long version) {
}
