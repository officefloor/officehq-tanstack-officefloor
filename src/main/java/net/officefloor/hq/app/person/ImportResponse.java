package net.officefloor.hq.app.person;

import java.util.List;

/** How many people the import added, with the sheet's people after. */
public record ImportResponse(int added, List<Person> people) {
}
