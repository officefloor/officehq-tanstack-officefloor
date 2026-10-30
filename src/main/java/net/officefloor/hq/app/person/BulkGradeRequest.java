package net.officefloor.hq.app.person;

import java.util.List;

/** Body of POST /api/sheets/{sheetId}/people/bulk-grade: the selected people's ids, the grade to give
 *  them, the date it is recorded from (YYYY-MM-DD, blank for the sheet's today) and the user giving it. */
public record BulkGradeRequest(List<String> personIds, String grade, String gradeFrom, String user) {
}
