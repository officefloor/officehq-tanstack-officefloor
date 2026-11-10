package net.officefloor.hq.app.person;

import java.util.List;

/** The bulk grade: the ids of the selected people, the grade (one of the sheet's grades) and its from-date (yyyy-mm-dd; blank for the sheet's today). */
public record BulkGradeRequest(List<Long> personIds, String grade, String from) {
}
