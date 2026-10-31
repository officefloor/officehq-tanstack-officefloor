package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk grade: the selected people's ids, the grade, the date it is in force from
 *  (yyyy-mm-dd; blank or null for the sheet's today) and who makes the change. */
public record BulkGradeRequest(List<Long> personIds, String grade, String from, String user) {
}
