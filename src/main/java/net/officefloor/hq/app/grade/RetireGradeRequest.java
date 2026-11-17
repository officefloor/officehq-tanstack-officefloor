package net.officefloor.hq.app.grade;

/** Body of a grade's retirement, and who makes it. */
public record RetireGradeRequest(String grade, String by) {
}
