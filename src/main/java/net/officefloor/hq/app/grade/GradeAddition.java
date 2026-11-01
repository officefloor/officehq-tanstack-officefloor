package net.officefloor.hq.app.grade;

/** A grade added to a sheet by `by`. */
public record GradeAddition(String sheetId, String grade, String by) {
}
