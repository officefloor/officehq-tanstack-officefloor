package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people and PUT /api/sheets/{sheetId}/people/{personId}.
 *  {@code version} (PUT only) is the version of the person the editor loaded; {@code gradeFrom}
 *  (PUT only, YYYY-MM-DD) is the date the grade is recorded from, blank for the sheet's today. */
public record PersonRequest(String name, String grade, String user, Integer version, String gradeFrom) {
}
