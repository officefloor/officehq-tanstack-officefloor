package net.officefloor.hq.app.grade;

/** A grade to add that passed validation: one to three capital letters, not yet a grade of the sheet. */
public record ValidGradeAdd(String grade, String user) {
}
