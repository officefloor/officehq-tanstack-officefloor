package net.officefloor.hq.app.person;

/** A person on a sheet: a name (kept as entered) and a grade, one of A, B or C. */
public record Person(long id, String sheetId, String name, String grade) {
}
