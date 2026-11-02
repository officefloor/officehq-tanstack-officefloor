package net.officefloor.hq.app.person;

import java.util.List;

/** How many people the bulk retire retired, and everyone on the sheet after it. */
public record BulkRetireResponse(int retired, List<Person> people) {
}
