package net.officefloor.hq.app.person;

/** A person on a sheet: name (unique on the sheet, ignoring case) and grade (A, B or C). */
public record Person(long id, String name, String grade) {
}
