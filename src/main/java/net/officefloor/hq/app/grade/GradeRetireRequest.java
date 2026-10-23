package net.officefloor.hq.app.grade;

/** Body of a grade retire: who is retiring it and the grade. */
public record GradeRetireRequest(String user, String grade) {
}
