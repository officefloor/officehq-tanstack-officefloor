package net.officefloor.hq.app.person;

import java.util.Set;

/** The people selected to be graded together, the grade, and its from-date (blank: the sheet's today). */
public record BulkGrade(Set<Long> ids, String grade, String from) {
}
