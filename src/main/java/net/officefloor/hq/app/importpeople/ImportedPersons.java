package net.officefloor.hq.app.importpeople;

import java.util.List;
import net.officefloor.hq.app.person.Person;

/** The imported people, built and checked but not yet saved, in order. */
public record ImportedPersons(List<Person> people) {
}
