package net.officefloor.hq.app.grade;

/** A grade retired on a sheet, and who retired it. */
public record GradeRetired(String sheetId, String grade, String by) {
}
