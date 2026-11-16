package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk grade: the ids of the selected people, the grade, the date it is given from (blank: the sheet's
 *  today), and who gives it. */
public record BulkGradeRequest(List<Long> personIds, String grade, String from, String by) {
}
