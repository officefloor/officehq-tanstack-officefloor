package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk grade: the ids of the selected people, in any order, the grade they are given and
 *  the date it applies from (blank: the sheet's today). */
public record BulkGradeRequest(List<Long> personIds, String grade, String from) {
}
