package net.officefloor.hq.app.grade;

/** Body of a grade added to or retired on the sheet: the grade and who adds or retires it. */
public record GradeRequest(String grade, String user) {
}
