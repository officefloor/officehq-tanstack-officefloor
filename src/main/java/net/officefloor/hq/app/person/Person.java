package net.officefloor.hq.app.person;

/** A person on a sheet: a name and a grade (A, B or C). */
public record Person(long id, String sheetId, String name, String grade) {
}
