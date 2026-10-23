package net.officefloor.hq.app.grade;

/** Body of a grade add: who is adding it and the grade's code. */
public record GradeAddRequest(String user, String grade) {
}
