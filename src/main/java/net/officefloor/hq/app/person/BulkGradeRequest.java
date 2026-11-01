package net.officefloor.hq.app.person;

import java.util.List;

/** Body of POST /api/sheets/{sheet}/people/bulk-grade: the ids of the selected people, the grade, and its from-date (blank for the sheet's today). */
public record BulkGradeRequest(List<Long> people, String grade, String from) {
}
