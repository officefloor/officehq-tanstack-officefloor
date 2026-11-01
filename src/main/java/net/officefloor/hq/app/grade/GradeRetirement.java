package net.officefloor.hq.app.grade;

/** A grade retired on a sheet by `by`. */
public record GradeRetirement(String sheetId, String grade, String by) {
}
