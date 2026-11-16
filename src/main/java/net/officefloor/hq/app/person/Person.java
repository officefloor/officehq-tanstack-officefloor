package net.officefloor.hq.app.person;

/** A person on a sheet: a name (unique on the sheet, case ignored; kept without surrounding spaces) and a grade, one of A, B or C. */
public record Person(long id, String sheetId, String name, String grade) {
}
