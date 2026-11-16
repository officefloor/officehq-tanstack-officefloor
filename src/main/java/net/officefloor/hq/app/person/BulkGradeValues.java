package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/** A bulk grade once validated: the selected people (each once), the grade, its from-date (null: the sheet's today),
 *  and who gives it. */
public record BulkGradeValues(List<Long> personIds, String grade, LocalDate from, String by) {
}
