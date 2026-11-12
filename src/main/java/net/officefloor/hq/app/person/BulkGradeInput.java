package net.officefloor.hq.app.person;

import java.util.List;

/** A validated bulk grade: the selected person ids, each once, the grade, its from-date (null for the
 *  sheet's today) and who gives it. */
public record BulkGradeInput(List<Long> personIds, String grade, String gradeFrom, String user) {
}
