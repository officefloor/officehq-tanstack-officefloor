package net.officefloor.hq.app.grade;

/** Body of a grade retired on the sheet: the grade and who retires it. */
public record GradeRequest(String grade, String user) {
}
