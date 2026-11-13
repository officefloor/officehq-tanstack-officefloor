package net.officefloor.hq.app.grade;

/** Body of POST /api/sheets/{sheetId}/grades/retire: the grade to retire and the user who retires it. */
public record GradeRetireRequest(String grade, String user) {
}
