package net.officefloor.hq.app.person;

import java.util.List;

/** How many people the bulk hold held, and everyone on the sheet after it. */
public record BulkHoldResponse(int held, List<Person> people) {
}
