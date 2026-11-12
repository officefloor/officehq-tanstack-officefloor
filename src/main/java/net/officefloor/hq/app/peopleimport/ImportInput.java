package net.officefloor.hq.app.peopleimport;

import java.util.List;
import net.officefloor.hq.app.person.PersonRequest;

/** A validated import: each person to add, as the person form would add them, in the order pasted. */
public record ImportInput(List<PersonRequest> people) {
}
