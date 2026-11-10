package net.officefloor.hq.app.person;

/** A person as the screen sends it, to add or to edit: a name and a grade (A, B or C). */
public record PersonRequest(String name, String grade) {
}
