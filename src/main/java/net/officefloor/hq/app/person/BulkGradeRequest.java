package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk grade: who is grading, the ids of the selected people, the grade and the date it applies from
 *  (yyyy-mm-dd; null or blank: the sheet's today). */
public record BulkGradeRequest(String user, List<Long> personIds, String grade, String from) {
}
