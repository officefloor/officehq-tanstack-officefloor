package net.officefloor.hq.app.person;

import java.util.List;

/**
 * A bulk grade as the screen sends it: the ids of the selected people, the grade (A, B or C) and
 * the date it is from (yyyy-MM-dd; blank for the sheet's today).
 */
public record BulkGradeRequest(List<String> personIds, String grade, String gradeFrom) {
}
