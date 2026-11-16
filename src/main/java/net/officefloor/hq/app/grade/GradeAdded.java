package net.officefloor.hq.app.grade;

/** A grade added to a sheet, and who added it. */
public record GradeAdded(String sheetId, String grade, String by) {
}
