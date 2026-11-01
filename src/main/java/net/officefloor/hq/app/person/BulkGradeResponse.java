package net.officefloor.hq.app.person;

import java.util.List;

/** How many people the bulk grade changed, and everyone on the sheet after it. */
public record BulkGradeResponse(int graded, List<Person> people) {
}
