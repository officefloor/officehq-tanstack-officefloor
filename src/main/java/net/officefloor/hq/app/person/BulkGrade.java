package net.officefloor.hq.app.person;

import java.util.Set;

/** A validated bulk grade: the selected people, and the grade change as the person form would send it. */
public record BulkGrade(Set<Long> personIds, String grade, String from, String user) {
}
