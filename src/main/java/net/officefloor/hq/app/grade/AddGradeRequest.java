package net.officefloor.hq.app.grade;

/** Body of a grade's addition: its code, and who adds it. */
public record AddGradeRequest(String grade, String by) {
}
