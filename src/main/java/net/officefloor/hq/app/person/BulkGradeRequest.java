package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk grade, as sent: the ids of the selected people, the grade (A, B or C), the date
 *  (yyyy-MM-dd, blank for the sheet's today) it applies from and who gives it. */
public record BulkGradeRequest(List<String> personIds, String grade, String gradeFrom, String user) {
}
