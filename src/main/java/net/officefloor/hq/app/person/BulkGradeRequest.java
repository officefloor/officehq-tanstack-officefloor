package net.officefloor.hq.app.person;

import java.util.List;

/** The selected people's ids to grade together, the grade, its from-date (blank or null: the sheet's today), and
 *  the signed-in user (`by`) giving it. */
public record BulkGradeRequest(List<Long> ids, String grade, String from, String by) {
}
