package net.officefloor.hq.app.person;

import java.util.List;

/** How many people the import added and how many it updated, with the sheet's people after. */
public record ImportResponse(int added, int updated, List<Person> people) {
}
