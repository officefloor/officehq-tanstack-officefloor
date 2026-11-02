package net.officefloor.hq.app.grade;

/** Body of POST /api/sheets/{sheet}/grades and /grades/retire: the grade added or retired. */
public record GradeRequest(String grade) {
}
