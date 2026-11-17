package net.officefloor.hq.app.grade;

/** A grade's retirement once validated: the grade, one of A, B or C, and who retires it. */
public record RetireGradeValues(String grade, String by) {
}
