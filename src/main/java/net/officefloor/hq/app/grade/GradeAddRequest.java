package net.officefloor.hq.app.grade;

/** Body of POST /api/sheets/{sheetId}/grades: the grade's code and the user who adds it. */
public record GradeAddRequest(String grade, String user) {
}
