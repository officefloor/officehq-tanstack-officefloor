package net.officefloor.hq.app.person;

import java.util.List;

/** How many people the grade merge moved, and everyone on the sheet after it. */
public record GradeMergeResponse(int merged, List<Person> people) {
}
