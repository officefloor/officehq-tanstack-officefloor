package net.officefloor.hq.app.grade;

/** Body of POST /api/sheets/{sheetId}/grades: the grade to add and the user adding it. */
public record GradeAddRequest(String grade, String user) {
}
