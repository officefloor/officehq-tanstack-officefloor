package net.officefloor.hq.app.importpeople;

import java.util.List;
import net.officefloor.hq.app.person.PersonRequest;

/** The imported lines as the person form validates them, in the order they were typed. */
public record ImportedPeople(List<PersonRequest> people) {
}
