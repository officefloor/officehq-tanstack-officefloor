package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.Set;

/** A validated bulk grade: who is grading, the selected people, the grade and its from-date (null: the sheet's today). */
public record BulkGradeInput(String user, Set<Long> personIds, String grade, LocalDate from) {
}
