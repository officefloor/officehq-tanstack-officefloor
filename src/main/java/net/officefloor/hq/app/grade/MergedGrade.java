package net.officefloor.hq.app.grade;

/** A grade being merged into another on a sheet. */
public record MergedGrade(String sheetId, String from, String into) {
}
