package net.officefloor.hq.app.grade;

/** A grade as the screen shows it: whether it is retired on the sheet. */
public record GradeResponse(String grade, boolean retired) {
}
